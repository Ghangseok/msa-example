package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 1.2: 같은 상품에 주문이 동시에 몰려도 초과 판매되지 않는다.
 * "주문 N건"은 서로 다른 주문 번호로 보낸 예약 요청 N건으로, "확정"은 RESERVED로, "거절"은 REJECTED로 읽는다.
 */
class ReservationConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-005 같은 상품에 주문이 동시에 몰려도 초과 판매되지 않는다")
    void doesNotOversellUnderConcurrentOrders() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);

        // When 서로 다른 주문 20건이 동시에 상품 A를 1개씩 주문한다
        List<Callable<Reply>> requests = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long orderNo = 5001L + i;
            requests.add(() -> reserve(orderNo, new Item("A", 1)));
        }
        List<Reply> replies = runConcurrently(requests);

        // Then "확정"은 정확히 10건, "거절"은 10건이다
        assertThat(replies.stream()
                        .filter(reply -> "RESERVED".equals(reply.result()))
                        .count())
                .isEqualTo(10);
        assertThat(replies.stream()
                        .filter(reply -> "REJECTED".equals(reply.result()))
                        .count())
                .isEqualTo(10);
        // And 상품 A의 재고는 0개이고 음수가 아니다
        assertThat(stockOf("A")).isZero();
    }
}
