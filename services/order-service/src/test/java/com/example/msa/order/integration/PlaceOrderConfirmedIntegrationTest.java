package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 2.1의 주문 쪽: 재고가 충분하면 여러 상품 주문이 확정된다.
 * 재고 서비스 자리(WireMock)가 RESERVED를 돌려준다. 재고 수량(A 7개, B 3개)은 재고 서비스 테스트가 본다.
 * 이 테스트는 주문 쪽을 본다. 응답, 저장된 주문, WireMock이 받은 예약 요청(횟수, 주문 번호, 항목)이다.
 */
class PlaceOrderConfirmedIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-001 재고가 충분하면 여러 상품 주문이 확정된다")
    void multiItemOrderIsConfirmed() {
        // Given 상품 A의 재고가 10개, 상품 B의 재고가 5개다. 재고 서비스 자리가 예약 결과 RESERVED를 돌려준다.
        stubReserve(reserved());

        // When 고객이 상품 A 3개와 상품 B 2개를 한 주문으로 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("B", 2));

        // Then 고객은 주문 번호와 "확정" 결과를 받는다 (새로 기록한 주문이라 201이다)
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("CONFIRMED");
        long orderNo = reply.orderNo();

        // And 그 주문이 C1의 주문으로 두 항목과 함께 "확정" 상태로 기록되어 있다
        Map<String, Object> order = orderRow(orderNo);
        assertThat(order).isNotNull();
        assertThat(order.get("CUSTOMER_ID")).isEqualTo(C1);
        assertThat(order.get("STATUS")).isEqualTo("CONFIRMED");
        assertThat(itemRows(orderNo))
                .extracting(row -> row.get("PRODUCT_ID"), row -> ((Number) row.get("QUANTITY")).intValue())
                .containsExactly(tuple("A", 3), tuple("B", 2));

        // And 재고 서비스 자리가 받은 예약 요청은 1번이고, 같은 주문 번호로 A 3개와 B 2개를 담았다
        List<LoggedRequest> requests = reserveRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getUrl()).isEqualTo("/reservations/" + orderNo);
        assertThat(itemsOf(requests.get(0))).containsExactlyInAnyOrder(new Item("A", 3), new Item("B", 2));
    }

    @Test
    @DisplayName("TC-001 변형: 상품 하나만 담은 주문도 같은 방식으로 확정된다")
    void singleItemOrderIsConfirmed() {
        stubReserve(reserved());

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("CONFIRMED");
        long orderNo = reply.orderNo();
        assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("CONFIRMED");
        assertThat(itemRows(orderNo))
                .extracting(row -> row.get("PRODUCT_ID"), row -> ((Number) row.get("QUANTITY")).intValue())
                .containsExactly(tuple("A", 3));
        List<LoggedRequest> requests = reserveRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getUrl()).isEqualTo("/reservations/" + orderNo);
        assertThat(itemsOf(requests.get(0))).containsExactly(new Item("A", 3));
    }
}
