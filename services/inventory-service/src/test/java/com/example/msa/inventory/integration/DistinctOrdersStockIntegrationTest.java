package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 6.1의 재고 쪽: 서로 다른 주문 번호의 예약은 각각 재고를 줄인다.
 * 서로 다른 주문 번호로 같은 내용의 예약 요청을 두 번 보내면 재고가 7개, 4개가 된다.
 */
class DistinctOrdersStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-011 같은 주문 요청 키로 다시 보내면 주문은 하나다: 재고 쪽은 서로 다른 주문 번호의 예약이 각각 재고를 줄인다")
    void distinctOrderNumbersEachReduceStock() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);

        // 첫 예약(상품 A 3개)
        assertThat(reserve(6001, new Item("A", 3)).result()).isEqualTo("RESERVED");
        // 상품 A의 재고는 7개다
        assertThat(stockOf("A")).isEqualTo(7);

        // 새 주문 번호로 같은 내용을 예약한다
        assertThat(reserve(6002, new Item("A", 3)).result()).isEqualTo("RESERVED");
        // 상품 A의 재고는 4개다
        assertThat(stockOf("A")).isEqualTo(4);
    }
}
