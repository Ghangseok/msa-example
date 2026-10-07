package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 2.1의 재고 쪽: 재고가 충분하면 여러 상품 예약이 되고 수량이 줄어든다.
 * 주문 서비스 테스트가 보내는 것과 같은 예약 요청(A 3개, B 2개)을 재고 서비스에 보내고 재고를 읽는다.
 */
class ConfirmedOrderStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-001 재고가 충분하면 여러 상품 주문이 확정된다: 재고 쪽은 A 7개, B 3개가 된다")
    void sufficientStockIsReservedForAllItems() {
        // Given 상품 A의 재고가 10개, 상품 B의 재고가 5개다
        insertStock("A", 10);
        insertStock("B", 5);

        // When 상품 A 3개와 상품 B 2개를 한 예약으로 요청한다. 주문 번호는 테스트가 정한 값이다.
        Reply reply = reserve(1, new Item("A", 3), new Item("B", 2));

        // Then 예약 결과는 RESERVED다("확정")
        assertThat(reply.result()).isEqualTo("RESERVED");
        // And 상품 A의 재고는 7개, 상품 B의 재고는 3개다
        assertThat(stockOf("A")).isEqualTo(7);
        assertThat(stockOf("B")).isEqualTo(3);
    }
}
