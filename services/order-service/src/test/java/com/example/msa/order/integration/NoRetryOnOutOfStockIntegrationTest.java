package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 3.2: 재고 부족 응답에는 재시도하지 않는다.
 * 이 테스트는 P3에서 재시도가 들어와도 고치지 않고 통과해야 한다. 그래서 테스트 케이스 문장에 있는 것만 단언한다.
 *
 * <p>P2에는 재시도가 없어서 예약 요청은 언제나 1번이다. 그래서 이 테스트는 P2에서 실패할 수 없다.
 * 실패 확인은 재시도를 만드는 Phase 3에서 한다.
 */
class NoRetryOnOutOfStockIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-009 재고 부족 응답에는 재시도하지 않는다")
    void outOfStockIsNotRetried() {
        // Given 상품 A의 재고가 2개다. 재고 서비스 자리가 REJECTED(부족한 상품 A)를 돌려준다.
        stubReserve(rejected("OUT_OF_STOCK", List.of("A"), List.of()));

        // When 고객이 상품 A를 3개 주문한다
        placeOrder(C1, newKey(), new Item("A", 3));

        // Then 예약 요청은 정확히 1번 갔다
        assertThat(reserveRequests()).hasSize(1);
    }
}
