package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 3.1의 재고 쪽: 항목 하나라도 재고가 부족하면 예약 전체가 거절되고, 어느 상품의 재고도 줄지 않는다.
 * 주문 서비스 테스트가 보내는 것과 같은 예약 요청(A 3개, B 2개)을 재고 서비스에 보내고 재고를 읽는다.
 */
class RejectedOrderStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다: 재고 쪽은 A도 줄지 않는다")
    void shortageOfOneItemRejectsWholeReservation() {
        // Given 상품 A의 재고가 10개, 상품 B의 재고가 1개다
        insertStock("A", 10);
        insertStock("B", 1);

        // When 상품 A 3개와 상품 B 2개를 한 예약으로 요청한다. 주문 번호는 테스트가 정한 값이다.
        Reply reply = reserve(2, new Item("A", 3), new Item("B", 2));

        // Then 예약 결과는 REJECTED("거절")이고, 부족한 상품은 B다
        assertThat(reply.result()).isEqualTo("REJECTED");
        assertThat(reply.texts("$.shortageProductIds")).isEqualTo(List.of("B"));
        // And 상품 A의 재고는 10개, 상품 B의 재고는 1개 그대로다 (A도 줄지 않는다)
        assertThat(stockOf("A")).isEqualTo(10);
        assertThat(stockOf("B")).isEqualTo(1);
    }
}
