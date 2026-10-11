package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 재고 서비스가 500을 돌려줄 때의 주문 쪽을 본다. 인수 시나리오 테스트가 아니라서 테스트 케이스 번호를 붙이지 않는다.
 *
 * <p>기대값은 테스트 케이스 문서 공통 전제의 일곱째 줄이다. "재고 서비스가 409나 500을 돌려주면 주문 서비스는 재시도하지
 * 않는다. 주문을 "실패"로 기록하고 재고 서비스에 해제를 요청한다." 스텁의 {@code code} 값(INTERNAL_ERROR)은 재고 서비스
 * 계약(contracts/inventory-api.yaml)의 500 응답이다. 기대값이 아니라 스텁을 만드는 값이다.
 *
 * <p>P3에서 재시도가 들어와도 이 테스트를 고치지 않고 통과해야 한다.
 */
class InventoryServerErrorIntegrationTest extends InventoryWireMock {

    /** 기다림의 안전장치다. 단언하는 값이 아니다. */
    private static final Duration SAFETY_LIMIT = Duration.ofSeconds(10);

    @Test
    @DisplayName("재고 서비스가 500을 돌려주면 재시도하지 않고 실패로 기록하고 해제를 요청한다")
    void serverErrorIsNotRetriedAndOrderFailsAndReleaseIsRequested() {
        // Given 재고 서비스 자리가 예약 요청에 500(INTERNAL_ERROR)을 돌려주고, 해제 요청에는 정상으로 응답한다
        stubReserve(problem(500, "INTERNAL_ERROR", "예상하지 못한 오류가 났다"));
        stubRelease(releaseResult("RELEASED"));

        // When 주문을 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));
        // 새로 기록한 주문은 결과(확정, 거절, 실패)와 관계없이 201이다
        assertThat(reply.status()).isEqualTo(201);
        long orderNo = reply.orderNo();

        // Then 예약 요청은 정확히 1번 갔다
        assertThat(reserveRequests()).hasSize(1);

        // And 주문은 "실패"로 기록된다
        assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("FAILED");

        // And 고객 응답 뒤 재고 서비스에 해제 요청이 간다
        await().atMost(SAFETY_LIMIT).until(() -> releaseRequests().size() == 1);
        List<LoggedRequest> releases = releaseRequests();
        assertThat(releases.get(0).getUrl()).isEqualTo("/reservations/" + orderNo);
    }
}
