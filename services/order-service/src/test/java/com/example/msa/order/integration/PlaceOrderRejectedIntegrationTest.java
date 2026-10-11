package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 3.1의 주문 쪽: 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다.
 * 재고 서비스 자리(WireMock)가 REJECTED(OUT_OF_STOCK, 부족한 상품 B)를 돌려준다.
 * "C1이 조회할 수 있다"는 티켓 002에서 검증하므로 넣지 않는다. 재고 수량(A 10개, B 1개 그대로)은 재고 서비스 테스트가 본다.
 */
class PlaceOrderRejectedIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다")
    void shortageRejectsTheWholeOrder() {
        // Given 상품 A의 재고가 10개, 상품 B의 재고가 1개다. 재고 서비스 자리가 REJECTED(부족한 상품 B)를 돌려준다.
        stubReserve(rejected("OUT_OF_STOCK", List.of("B"), List.of()));

        // When 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("B", 2));

        // Then 고객은 주문 번호와 "거절", 부족한 상품 B를 받는다
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.texts("$.shortageProductIds")).containsExactly("B");
        long orderNo = reply.orderNo();

        // And 그 주문이 "거절" 상태로 기록되어 있다
        Map<String, Object> order = orderRow(orderNo);
        assertThat(order).isNotNull();
        assertThat(order.get("CUSTOMER_ID")).isEqualTo(C1);
        assertThat(order.get("STATUS")).isEqualTo("REJECTED");

        // And 재고 서비스 자리가 받은 예약 요청은 1번이고 A 3개와 B 2개를 담았다 (A도 함께 요청해서 재고 서비스가 둘 다 줄이지 않는다)
        List<LoggedRequest> requests = reserveRequests();
        assertThat(requests).hasSize(1);
        assertThat(itemsOf(requests.get(0))).containsExactlyInAnyOrder(new Item("A", 3), new Item("B", 2));
    }
}
