package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 시나리오 9.1의 주문 쪽: 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다.
 *
 * <p>재고 쪽 기대값("상품 A의 재고는 10개다")은 재고 서비스 테스트(ReleaseRestoresStockIntegrationTest)가 본다.
 * 이 테스트는 주문 쪽을 본다. 예약 요청 횟수, 고객 응답 시간, 실패 기록, 해제 요청과 해제 완료, 그동안의 health 응답을 본다.
 *
 * <p>시간은 시간 값을 1/5로 줄인 테스트 설정을 따른다 [제안]. 재고 서비스의 응답 "3초 뒤"는 600ms로, "31초 안"은 6.2초 안으로 줄였다.
 * "고객 응답 뒤 5초 안에"와 health 확인 "1초 안에 200"은 줄이지 않는다 [제안]. 설정한 시간과 관계없는 응답 시간이기 때문이다.
 * 시도당 제한 시간(500ms)보다 긴 600ms 지연이라서, 여섯 시도는 모두 시간 초과로 끝난다.
 */
class OverallTimeLimitIntegrationTest extends InventoryWireMock {

    private static final Logger log = LoggerFactory.getLogger(OverallTimeLimitIntegrationTest.class);

    /** "고객은 31초 안에 주문 번호와 '잠시 후 다시 시도'를 받는다"를 1/5로 줄인 값이다(30초 + 허용 오차 1초 = 31초 → 6.2초). */
    private static final Duration RESPONSE_LIMIT = Duration.ofMillis(6200);

    /** "고객 응답 뒤 5초 안에 재고 서비스에 해제 요청이 가고, 주문에 '해제 완료'가 기록된다". 줄이지 않는다. */
    private static final Duration RELEASE_LIMIT = Duration.ofSeconds(5);

    /** "주문 서비스의 health 확인은 1초 안에 200으로 응답한다". 줄이지 않는다. */
    private static final Duration HEALTH_LIMIT = Duration.ofSeconds(1);

    @Test
    @DisplayName("TC-004 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다")
    void failsWithinTheOverallLimitAndReleasesTheReservation() throws InterruptedException, ExecutionException {
        // Given 재고 서비스가 예약 요청을 처리하지만, 응답은 3초(테스트에서는 600ms) 뒤에 준다. 시도당 제한 시간보다 길다
        stubReserve(reserved().withFixedDelay(600));
        // And 재고 서비스는 해제 요청에는 바로 정상으로 응답한다
        stubRelease(releaseResult("RELEASED"));

        // And 주문 서비스의 health 확인을 주문하는 동안 다른 스레드에서 계속 부른다
        AtomicBoolean ordering = new AtomicBoolean(true);
        List<HealthCheck> healthChecks = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<Void> healthPolling = CompletableFuture.runAsync(() -> {
            while (ordering.get()) {
                healthChecks.add(checkHealth());
                pause(Duration.ofMillis(100));
            }
        });

        // When 고객이 상품 A를 3개 주문한다
        long started = System.nanoTime();
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
        ordering.set(false);
        healthPolling.get();
        log.info("시나리오 9.1 고객 응답 시간: {} ms", elapsed.toMillis());

        // Then 고객은 31초(테스트에서는 6.2초) 안에 주문 번호와 "잠시 후 다시 시도"를 받는다
        assertThat(elapsed).isLessThan(RESPONSE_LIMIT);
        assertThat(reply.status()).isEqualTo(201);
        long orderNo = reply.orderNo();
        assertThat(reply.text("$.status")).isEqualTo("FAILED");
        assertThat(reply.text("$.reason")).isEqualTo("RETRY_LATER");

        // And 예약 요청은 정확히 6번 갔다
        assertThat(reserveRequests()).hasSize(6);

        // And 그 주문은 "실패" 상태로 기록되어 있다
        assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("FAILED");

        // And 고객 응답 뒤 5초 안에 재고 서비스에 해제 요청이 가고, 주문에 "해제 완료"가 기록된다
        await().atMost(RELEASE_LIMIT).untilAsserted(() -> {
            assertThat(releaseRequests()).hasSize(1);
            assertThat(releaseRequests().get(0).getUrl()).isEqualTo("/reservations/" + orderNo);
            assertThat(orderRow(orderNo).get("RELEASE_STATUS")).isEqualTo("RELEASED");
        });

        // And 그동안 주문 서비스의 health 확인은 1초 안에 200으로 응답한다
        assertThat(healthChecks).as("주문하는 동안 health 확인을 부른 횟수").hasSizeGreaterThanOrEqualTo(3);
        Duration slowestHealth = healthChecks.stream()
                .map(HealthCheck::elapsed)
                .max(Duration::compareTo)
                .orElseThrow();
        log.info("시나리오 9.1 health 확인 {}번 중 가장 느린 응답 시간: {} ms", healthChecks.size(), slowestHealth.toMillis());
        assertThat(healthChecks).allSatisfy(check -> assertThat(check.status()).isEqualTo(200));
        assertThat(slowestHealth).isLessThan(HEALTH_LIMIT);
    }

    /** health 확인 한 번의 응답 코드와 걸린 시간이다. */
    private record HealthCheck(int status, Duration elapsed) {}

    private HealthCheck checkHealth() {
        long started = System.nanoTime();
        try {
            int status = mvc.perform(get("/actuator/health"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            return new HealthCheck(status, Duration.ofNanos(System.nanoTime() - started));
        } catch (Exception e) {
            log.warn("health 확인을 보내지 못했다", e);
            return new HealthCheck(-1, Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
