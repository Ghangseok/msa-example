package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 시나리오 7.1: 재고 서비스 실패가 이어지면 서킷이 열려 바로 실패한다.
 *
 * <p>P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다. 그래서 테스트 케이스 문장에 있는 것만 단언한다.
 * 주문마다 예약 요청이 몇 번 갔는지, 몇 번째 주문에서 서킷이 열리는지는 단언하지 않는다 [제안].
 * 서킷이 열린 뒤의 첫 주문은 "WireMock이 받은 예약 요청 수가 늘지 않은 주문"으로 알아본다.
 *
 * <p>"해제 실패"는 해제가 비동기라서 바로 기록되지 않는다. 정해진 시간까지 저장된 주문을 다시 읽는 방식으로 기다린다 [제안].
 * 기다리는 한도는 테스트 케이스에 값이 없어 단언하지 않고, 테스트가 끝없이 돌지 않게 하는 안전장치로만 둔다(10초) [제안].
 * 열림 시간은 시간 값을 1/5로 줄인 테스트 설정(2초)이다. "1초 안에"는 줄이지 않는다 [제안].
 */
class CircuitBreakerIntegrationTest extends InventoryWireMock {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerIntegrationTest.class);

    /** 기다림의 안전장치다. 단언하는 값이 아니다. */
    private static final Duration SAFETY_LIMIT = Duration.ofSeconds(10);

    /** 서킷의 열림 시간(테스트 설정 2초)이 지나기를 기다리는 시간이다. 2초보다 조금 길게 둔다. */
    private static final Duration OPEN_DURATION_PASSED = Duration.ofMillis(2300);

    /** 서킷이 열릴 때까지 보내 보는 주문의 최대 개수다. 열림 조건은 최근 10번 중 50% 이상 실패다. 한참 넉넉하게 둔다. */
    private static final int MAX_ORDERS_UNTIL_OPEN = 50;

    @Test
    @DisplayName("TC-013 재고 서비스 실패가 이어지면 서킷이 열려 바로 실패한다")
    void circuitOpensAndFailsFast() throws InterruptedException {
        // Given 재고 서비스가 모든 예약 요청과 해제 요청에 503을 돌려준다
        stubReserve(status(503));
        stubRelease(status(503));

        // When 고객들이 주문을 연달아 요청한다. 서킷이 열려 예약 요청이 더 가지 않는 주문이 나올 때까지 보낸다.
        List<Long> sentBeforeOpen = new ArrayList<>();
        Reply firstOpenReply = null;
        Duration firstOpenElapsed = null;
        for (int i = 0; i < MAX_ORDERS_UNTIL_OPEN && firstOpenReply == null; i++) {
            int requestsBefore = reserveRequests().size();
            long started = System.nanoTime();
            Reply reply = placeOrder(C1, newKey(), new Item("A", 1));
            Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
            assertThat(reply.status()).isEqualTo(201);
            assertThat(reply.text("$.status")).isEqualTo("FAILED");
            assertThat(reply.text("$.reason")).isEqualTo("RETRY_LATER");
            if (reserveRequests().size() == requestsBefore) {
                firstOpenReply = reply;
                firstOpenElapsed = elapsed;
            } else {
                sentBeforeOpen.add(reply.orderNo());
            }
        }

        // Then 실패한 시도가 서킷의 열림 조건에 이르면 서킷이 열린다
        assertThat(firstOpenReply).as("서킷이 열려 예약 요청이 더 가지 않는 주문").isNotNull();

        // And 그 뒤의 주문은 재고 서비스에 요청을 보내지 않고 1초 안에 "실패"와 "잠시 후 다시 시도"를 받는다
        log.info("서킷이 열린 뒤 첫 주문의 응답 시간: {} ms", firstOpenElapsed.toMillis());
        assertThat(firstOpenElapsed).isLessThan(Duration.ofSeconds(1));
        List<Long> failedFast = new ArrayList<>(List.of(firstOpenReply.orderNo()));
        int requestsWhileOpen = reserveRequests().size();
        for (int i = 0; i < 3; i++) {
            long started = System.nanoTime();
            Reply reply = placeOrder(C1, newKey(), new Item("A", 1));
            Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
            log.info("서킷이 열려 있는 동안 주문의 응답 시간: {} ms", elapsed.toMillis());
            assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
            assertThat(reply.text("$.status")).isEqualTo("FAILED");
            assertThat(reply.text("$.reason")).isEqualTo("RETRY_LATER");
            failedFast.add(reply.orderNo());
        }
        assertThat(reserveRequests()).hasSize(requestsWhileOpen);

        // And 그 주문들은 "해제 불필요"로 기록되고 해제 요청도 가지 않는다
        for (long orderNo : failedFast) {
            assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("FAILED");
            assertThat(orderRow(orderNo).get("RELEASE_STATUS")).isEqualTo("NOT_REQUIRED");
        }
        assertThat(releaseRequests().stream().map(CircuitBreakerIntegrationTest::orderNoOf))
                .doesNotContainAnyElementsOf(failedFast);

        // And 서킷이 열리기 전에 예약 요청을 보낸 주문은 "실패"로 기록되고, 재고 서비스가 계속 503을 돌려주는 동안 해제도 실패해 "해제 실패"로 기록된다
        await().atMost(SAFETY_LIMIT).untilAsserted(() -> {
            for (long orderNo : sentBeforeOpen) {
                Map<String, Object> order = orderRow(orderNo);
                assertThat(order.get("STATUS")).isEqualTo("FAILED");
                assertThat(order.get("RELEASE_STATUS")).isEqualTo("RELEASE_FAILED");
            }
        });

        // When 재고 서비스가 정상으로 돌아오고 서킷의 열림 시간이 지난다
        stubReserve(reserved());
        stubRelease(releaseResult("RELEASED"));
        Thread.sleep(OPEN_DURATION_PASSED.toMillis());

        // Then 시험 호출이 성공해 서킷이 닫히고, 새 주문이 "확정"된다
        Reply recovered = placeOrder(C1, newKey(), new Item("A", 1));
        assertThat(recovered.status()).isEqualTo(201);
        assertThat(recovered.text("$.status")).isEqualTo("CONFIRMED");
        assertThat(orderRow(recovered.orderNo()).get("STATUS")).isEqualTo("CONFIRMED");
    }

    private static long orderNoOf(LoggedRequest request) {
        String url = request.getUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }
}
