package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 7.2의 주문 쪽: 같은 주문 번호에 다른 내용이 오면 거부한다.
 * 주문 서비스는 재고 서비스가 요청 키 충돌(409)을 돌려줘도 같은 일이 정상으로 생기지 않으므로, 재고 서비스 자리(WireMock)가
 * 409를 돌려주게 해서 상황을 만든다. 이 스토리에서 검증하는 줄은 재시도하지 않는 것, "실패"로 기록하는 것, 해제 요청이 가는 것이다.
 *
 * <p>P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다.
 */
class ReservationConflictOrderIntegrationTest extends InventoryWireMock {

    /** 기다림의 안전장치다. 단언하는 값이 아니다. */
    private static final Duration SAFETY_LIMIT = Duration.ofSeconds(10);

    @Test
    @DisplayName("TC-008 같은 주문 번호에 다른 내용이 오면 재시도하지 않고 실패로 기록하고 해제를 요청한다")
    void conflictIsNotRetriedAndOrderFailsAndReleaseIsRequested() {
        // Given 재고 서비스 자리가 예약 요청에 요청 키 충돌(409)을 돌려주고, 해제 요청에는 정상으로 응답한다
        stubReserve(problem(409, "RESERVATION_CONFLICT", "같은 주문 번호에 다른 내용의 예약 요청이 왔다"));
        stubRelease(releaseResult("RELEASED"));

        // When 주문을 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));
        // 새로 기록한 주문은 결과(확정, 거절, 실패)와 관계없이 201이다
        assertThat(reply.status()).isEqualTo(201);
        long orderNo = reply.orderNo();

        // Then 주문 서비스는 이 응답에 재시도하지 않는다: 예약 요청은 정확히 1번 갔다
        assertThat(reserveRequests()).hasSize(1);

        // And 그 주문은 "실패"로 기록된다
        assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("FAILED");

        // And 고객 응답 뒤 재고 서비스에 해제 요청이 간다
        await().atMost(SAFETY_LIMIT).until(() -> releaseRequests().size() == 1);
        List<LoggedRequest> releases = releaseRequests();
        assertThat(releases.get(0).getUrl()).isEqualTo("/reservations/" + orderNo);
    }
}
