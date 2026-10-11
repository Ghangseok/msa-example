package com.example.msa.order.client;

import com.example.msa.order.dto.OrderItemRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
 * 업무 결과를 얻지 못하면 {@link InventoryCallFailure}를 던진다. P2에는 재시도가 없어서 호출 하나가 시도 하나다.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public InventoryClient(RestClient inventoryRestClient, CircuitBreaker inventoryCircuitBreaker) {
        this.restClient = inventoryRestClient;
        this.circuitBreaker = inventoryCircuitBreaker;
    }

    /** 해제 결과다. contracts/inventory-api.yaml의 ReleaseResult와 같다. 거절된 예약은 REJECTED 그대로다. */
    public record ReleaseResult(long orderNo, ReservationResult.Status status) {}

    /** 주문 항목 전체의 재고를 한꺼번에 예약한다. 200 응답의 업무 결과(RESERVED, REJECTED, RELEASED)를 돌려준다. */
    public ReservationResult reserve(long orderNo, List<OrderItemRequest> items, InventoryCallContext context) {
        return call(
                "예약",
                orderNo,
                context,
                () -> restClient
                        .put()
                        .uri("/reservations/{orderNo}", orderNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ReserveRequest(items))
                        .retrieve()
                        .body(ReservationResult.class));
    }

    /** 예약을 해제해 줄였던 재고를 되돌린다. 200 응답의 해제 결과(RELEASED, 거절된 예약이면 REJECTED)를 돌려준다. */
    public ReleaseResult release(long orderNo, InventoryCallContext context) {
        return call(
                "해제",
                orderNo,
                context,
                () -> restClient
                        .delete()
                        .uri("/reservations/{orderNo}", orderNo)
                        .retrieve()
                        .body(ReleaseResult.class));
    }

    /** 시도 하나를 서킷의 허가를 받아 보내고, 결과를 서킷에 기록한다. */
    private <T> T call(String operation, long orderNo, InventoryCallContext context, Supplier<T> request) {
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
        long started = System.nanoTime();
        try {
            T result = request.get();
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
