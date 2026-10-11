package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 8.2: 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다.
 *
 * <p>테스트 케이스의 비고처럼, 첫 주문의 해제 요청도 같은 서킷을 지나가므로 둘째 주문의 요청이 정확히 몇 번인지는 해제가 언제
 * 끼어드는지에 따라 달라진다. 그래서 "6번보다 적다"로 판정한다. 첫 주문의 "예약 요청 6번"은 단언한다. 이 단언이 없으면 재시도가
 * 없는 단계에서도 "6번보다 적다"가 저절로 참이 된다.
 *
 * <p>시간은 시간 값을 1/5로 줄인 테스트 설정을 따른다 [제안]. 첫 주문은 대기 0, 200, 400, 800, 1600ms를 거쳐 약 3초 걸리고,
 * 전체 한도 6초 안에 끝난다.
 */
class CircuitOpensDuringRetryIntegrationTest extends InventoryWireMock {

    /** 기다림의 안전장치다. 단언하는 값이 아니다. */
    private static final Duration SAFETY_LIMIT = Duration.ofSeconds(15);

    @Test
    @DisplayName("TC-018 재시도하던 중에 서킷이 열리면 남은 재시도를 하지 않는다")
    void remainingRetriesAreSkippedOnceTheCircuitOpens() {
        // Given 재고 서비스가 모든 예약 요청과 해제 요청에 503을 돌려준다
        stubReserve(status(503));
        stubRelease(status(503));

        // And 고객 C1의 첫 주문이 예약 요청 6번을 모두 실패하고 "실패"로 끝났다
        Reply first = placeOrder(C1, newKey(), new Item("A", 3));
        assertThat(first.text("$.status")).isEqualTo("FAILED");
        assertThat(reserveRequests()).hasSize(6);

        // When 고객 C1이 둘째 주문을 요청한다
        Reply second = placeOrder(C1, newKey(), new Item("A", 3));
        int reserveRequestsOfSecond = reserveRequests().size() - 6;

        // Then 둘째 주문의 예약 요청은 6번보다 적게 간다. 서킷이 열린 뒤에는 남은 재시도를 하지 않는다
        assertThat(reserveRequestsOfSecond).isLessThan(6);

        // And 둘째 주문은 "실패"로 기록되고 고객은 "잠시 후 다시 시도"를 받는다
        assertThat(second.status()).isEqualTo(201);
        assertThat(second.text("$.status")).isEqualTo("FAILED");
        assertThat(second.text("$.reason")).isEqualTo("RETRY_LATER");
        assertThat(orderRow(second.orderNo()).get("STATUS")).isEqualTo("FAILED");

        // 두 주문의 해제가 끝나기를 기다린다. 단언이 아니다. 해제가 다음 테스트의 스텁에 요청을 보내지 않게 하려는 것이다.
        // 둘째 주문은 서킷이 처음부터 열려 있었다면 "해제 불필요"가 바로 기록되어 있다.
        await().atMost(SAFETY_LIMIT)
                .until(() -> orderRow(first.orderNo()).get("RELEASE_STATUS") != null
                        && orderRow(second.orderNo()).get("RELEASE_STATUS") != null);
    }
}
