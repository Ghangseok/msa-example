package com.example.msa.order.client;

import com.example.msa.order.config.InventoryClientProperties;
import com.example.msa.order.dto.OrderItemRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 재고 서비스를 부르는 유일한 곳이다. 예약은 {@code PUT /reservations/{orderNo}}, 해제는 {@code DELETE /reservations/{orderNo}}다.
 * 주문 번호가 재고 예약의 멱등 키라서, 같은 주문 번호로 다시 불러도 재고는 한 번만 줄거나 한 번만 돌아온다.
 * 기준은 저장소 루트 contracts/inventory-api.yaml이다.
 *
 * <p>시도마다 서킷 브레이커의 허가를 받고 결과를 기록한다. 서킷이 열려 있으면 요청을 보내지 않고 바로 실패한다.
 * 실패로 세는 것은 연결 실패, 시도 제한 시간 초과, 5xx다. 업무 결과(RESERVED, REJECTED, RELEASED)와 4xx(409 포함)는 성공으로 센다.
 * 업무 결과를 얻지 못하면 {@link InventoryCallFailure}를 던진다.
 *
 * <p>재시도는 서킷 바깥에서 감싼다. "재시도(서킷 브레이커(재고 호출))" 순서라서 서킷이 시도 하나하나를 센다.
 * 재시도하는 것은 연결 실패, 시도 제한 시간 초과, 502·503·504뿐이다. 업무 결과, 409, 500, 그 밖의 4xx, 서킷이 열려 막힌 호출은
 * 재시도하지 않는다. 재시도할 때도 같은 주문 번호를 보낸다. 요청마다 {@link Retry}를 새로 만든다.
 *
 * <p>재시도를 포함한 전체 한도는 {@link InventoryCallContext}가 판단한다. 다음 시도를 대기 뒤에 시작해도 30초가 되기 전일 때만
 * 재시도한다. 시도마다 제한 시간은 시도당 제한 시간과 전체 한도까지 남은 시간 가운데 짧은 쪽이다. 그래서 진행 중인 시도도
 * 전체 한도에서 끊긴다. 한도 없이 만든 문맥(기본 생성자)으로 부르면 시도 하나만 보낸다.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    /** 재시도하는 HTTP 상태 코드다. 재고 앱이 아니라 네트워크와 인프라가 돌려주는 일시 오류다. */
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(502, 503, 504);

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration overallTimeLimit;
    private final RetrySchedule retrySchedule;
    private final Clock clock = Clock.systemUTC();

    public InventoryClient(
            RestClient inventoryRestClient,
            HttpClient inventoryHttpClient,
            CircuitBreaker inventoryCircuitBreaker,
            InventoryClientProperties properties) {
        this.restClient = inventoryRestClient;
        this.httpClient = inventoryHttpClient;
        this.circuitBreaker = inventoryCircuitBreaker;
        this.requestTimeout = properties.requestTimeout();
        this.maxRetries = properties.maxRetries();
        this.overallTimeLimit = properties.overallTimeLimit();
        this.retrySchedule = new RetrySchedule(properties.retryWaits());
    }

    /** 재시도를 포함한 전체 한도가 있는 호출 문맥을 만든다. 예약 한 번, 해제 한 번마다 새로 만든다. 지금 시각이 시작 시각이다. */
    public InventoryCallContext newCallContext() {
        return new InventoryCallContext(clock, overallTimeLimit);
    }

    /** 해제 결과다. contracts/inventory-api.yaml의 ReleaseResult와 같다. 거절된 예약은 REJECTED 그대로다. */
    public record ReleaseResult(long orderNo, ReservationResult.Status status) {}

    /** 주문 항목 전체의 재고를 한꺼번에 예약한다. 200 응답의 업무 결과(RESERVED, REJECTED, RELEASED)를 돌려준다. */
    public ReservationResult reserve(long orderNo, List<OrderItemRequest> items, InventoryCallContext context) {
        return callWithRetry(
                "예약",
                orderNo,
                context,
                client -> client.put()
                        .uri("/reservations/{orderNo}", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ReserveRequest(items))
                        .retrieve()
                        .body(ReservationResult.class));
    }

    /** 예약을 해제해 줄였던 재고를 되돌린다. 200 응답의 해제 결과(RELEASED, 거절된 예약이면 REJECTED)를 돌려준다. */
    public ReleaseResult release(long orderNo, InventoryCallContext context) {
        return callWithRetry(
                "해제",
                orderNo,
                context,
                client -> client.delete()
                        .uri("/reservations/{orderNo}", orderNo)
                        .retrieve()
                        .body(ReleaseResult.class));
    }

    /**
     * 문맥이 재시도를 허락하면 시도를 재시도로 감싸서 보낸다. 요청마다 {@link Retry}를 새로 만들고, 대기 일정은 공유한다.
     * 재시도를 마치고도 업무 결과를 얻지 못하면 마지막 시도의 {@link InventoryCallFailure}를 던진다.
     */
    private <T> T callWithRetry(
            String operation, long orderNo, InventoryCallContext context, Function<RestClient, T> request) {
        Supplier<T> attempt = () -> call(operation, orderNo, context, request);
        if (!context.retriesAllowed()) {
            return attempt.get();
        }
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxRetries + 1)
                .intervalBiFunction(retrySchedule.intervalFunction())
                .retryOnException(error -> isTransient(error) && startsBeforeOverallLimit(context))
                .build();
        Retry retry = Retry.of("inventory-" + operation, config);
        return Retry.decorateSupplier(retry, attempt).get();
    }

    /**
     * 다음 재시도를 대기 뒤에 시작해도 전체 한도가 되기 전인가. 방금 실패한 시도가 몇 번째였는지가 곧 이번 재시도의 번호이고,
     * 그 번호의 대기가 다음 재시도 전의 대기다. 최대 횟수를 넘은 재시도는 없다.
     */
    private boolean startsBeforeOverallLimit(InventoryCallContext context) {
        int retryNumber = context.sentAttempts();
        if (retryNumber > maxRetries) {
            return false;
        }
        return context.allowsRetryAfterWaiting(Duration.ofMillis(retrySchedule.waitMillisBeforeRetry(retryNumber)));
    }

    /** 재시도할 일시 오류인가. 연결 실패, 시도 제한 시간 초과, 502·503·504만 그렇다. */
    private static boolean isTransient(Throwable error) {
        if (!(error instanceof InventoryCallFailure failure)) {
            return false;
        }
        return switch (failure.kind()) {
            case CONNECTION_FAILURE, TIMEOUT -> true;
            case HTTP_STATUS -> RETRYABLE_STATUSES.contains(failure.httpStatus());
            case CIRCUIT_OPEN, UNEXPECTED -> false;
        };
    }

    /**
     * 시도 하나를 서킷의 허가를 받아 보내고, 결과를 서킷에 기록한다.
     * 시도의 제한 시간은 시도당 제한 시간과 전체 한도까지 남은 시간 가운데 짧은 쪽이다.
     */
    private <T> T call(String operation, long orderNo, InventoryCallContext context, Function<RestClient, T> request) {
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException e) {
            log.warn(
                    "재고 {} 호출을 서킷이 막았다. 요청을 보내지 않는다. orderNo={} circuit={}",
                    operation,
                    orderNo,
                    circuitBreaker.getState());
            throw new InventoryCallFailure(
                    InventoryCallFailure.Kind.CIRCUIT_OPEN, 0, "재고 " + operation + " 호출을 서킷이 막았다", e);
        }
        int attempt = context.recordSentAttempt();
        RestClient client = restClientWithin(context.timeoutForNextAttempt(requestTimeout));
        long started = System.nanoTime();
        try {
            T result = request.apply(client);
            circuitBreaker.onSuccess(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            log.info(
                    "재고 {} 호출이 끝났다. orderNo={} attempt={} circuit={}",
                    operation,
                    orderNo,
                    attempt,
                    circuitBreaker.getState());
            return result;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status >= 500) {
                circuitBreaker.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, e);
            } else {
                circuitBreaker.onSuccess(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            }
            log.warn(
                    "재고 {} 호출이 {}로 끝났다. orderNo={} attempt={} circuit={}",
                    operation,
                    status,
                    orderNo,
                    attempt,
                    circuitBreaker.getState());
            throw new InventoryCallFailure(
                    InventoryCallFailure.Kind.HTTP_STATUS, status, "재고 " + operation + " 호출이 " + status + "로 끝났다", e);
        } catch (ResourceAccessException e) {
            circuitBreaker.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, e);
            InventoryCallFailure.Kind kind = causedByTimeout(e)
                    ? InventoryCallFailure.Kind.TIMEOUT
                    : InventoryCallFailure.Kind.CONNECTION_FAILURE;
            log.warn(
                    "재고 {} 호출이 실패했다. kind={} orderNo={} attempt={} circuit={}",
                    operation,
                    kind,
                    orderNo,
                    attempt,
                    circuitBreaker.getState());
            throw new InventoryCallFailure(kind, 0, "재고 " + operation + " 호출이 실패했다(" + kind + ")", e);
        } catch (RuntimeException e) {
            circuitBreaker.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, e);
            log.warn(
                    "재고 {} 호출 결과를 처리하지 못했다. orderNo={} attempt={} circuit={}",
                    operation,
                    orderNo,
                    attempt,
                    circuitBreaker.getState(),
                    e);
            throw new InventoryCallFailure(
                    InventoryCallFailure.Kind.UNEXPECTED, 0, "재고 " + operation + " 호출 결과를 처리하지 못했다", e);
        }
    }

    /**
     * 시도 하나에 줄 제한 시간으로 요청을 보낼 {@link RestClient}를 고른다. 시도당 제한 시간 그대로면 설정으로 만든 {@code RestClient}를
     * 쓴다. 전체 한도까지 남은 시간이 더 짧으면, 같은 설정에서 요청 제한 시간만 그 시도의 남은 시간으로 줄인 {@code RestClient}를 만든다.
     * JDK HttpClient의 요청 제한 시간은 요청 팩토리에 걸려 있어서 요청마다 바꿀 수 없기 때문이다. 연결을 맺는 시간도 이 요청 제한 시간에
     * 들어간다(InventoryClientTimeoutIntegrationTest가 확인한다).
     */
    private RestClient restClientWithin(Duration attemptTimeout) {
        if (attemptTimeout.compareTo(requestTimeout) >= 0) {
            return restClient;
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(attemptTimeout);
        return restClient.mutate().requestFactory(requestFactory).build();
    }

    /** 원인 사슬에 JDK HttpClient의 시간 초과(연결 시간 초과 포함)가 있는가. */
    private static boolean causedByTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** 재고 서비스의 예약 요청 본문이다. contracts/inventory-api.yaml의 ReserveRequest와 같다. */
    private record ReserveRequest(List<OrderItemRequest> items) {}
}
