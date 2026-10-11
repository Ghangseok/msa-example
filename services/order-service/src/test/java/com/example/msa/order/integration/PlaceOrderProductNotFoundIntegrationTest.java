package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 3.3의 주문 쪽: 없는 상품이 섞이면 주문 전체가 거절되고 기록된다.
 * 재고 서비스 자리(WireMock)가 REJECTED(PRODUCT_NOT_FOUND)를 돌려준다. 재고 수량은 재고 서비스 테스트가 본다.
 * 본 흐름과 변형을 메서드 두 개로 나눈다. 어느 부분이 실패했는지 바로 보이게 하려는 것이다.
 */
class PlaceOrderProductNotFoundIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-015 없는 상품이 섞이면 주문 전체가 거절되고 기록된다")
    void missingProductRejectsTheWholeOrder() {
        // Given 상품 A의 재고가 10개다. 상품 Z는 재고 데이터에 없다. 재고 서비스 자리가 REJECTED(없는 상품 Z)를 돌려준다.
        stubReserve(rejected("PRODUCT_NOT_FOUND", List.of(), List.of("Z")));

        // When 고객이 상품 A 3개와 상품 Z 1개를 한 주문으로 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("Z", 1));

        // Then 고객은 주문 번호와 "거절", 사유 "상품 없음", 없는 상품 Z를 받는다
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reply.texts("$.missingProductIds")).containsExactly("Z");

        // And 그 주문이 "거절" 상태로 기록되어 있다
        Map<String, Object> order = orderRow(reply.orderNo());
        assertThat(order).isNotNull();
        assertThat(order.get("STATUS")).isEqualTo("REJECTED");

        // And 예약 요청은 정확히 1번 갔다 (재시도하지 않는다)
        assertThat(reserveRequests()).hasSize(1);
    }

    @Test
    @DisplayName("TC-015 변형: 없는 상품 Z와 부족한 상품 B가 함께 있으면 사유는 상품 없음이고 두 목록을 모두 받는다")
    void missingAndShortageProductsAreBothReported() {
        // Given 상품 B의 재고가 1개다. 재고 서비스 자리가 REJECTED(사유 상품 없음, 없는 상품 Z, 부족한 상품 B)를 돌려준다.
        stubReserve(rejected("PRODUCT_NOT_FOUND", List.of("B"), List.of("Z")));

        // When 상품 A 3개, 상품 Z 1개, 상품 B 2개를 한 주문으로 요청한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("Z", 1), new Item("B", 2));

        // Then 사유는 "상품 없음"이고 없는 상품 Z와 부족한 상품 B를 모두 받는다
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reply.texts("$.missingProductIds")).containsExactly("Z");
        assertThat(reply.texts("$.shortageProductIds")).containsExactly("B");
        assertThat(orderRow(reply.orderNo()).get("STATUS")).isEqualTo("REJECTED");

        // And 예약 요청은 정확히 1번 갔고 세 항목을 담았다
        List<LoggedRequest> requests = reserveRequests();
        assertThat(requests).hasSize(1);
        assertThat(itemsOf(requests.get(0)))
                .containsExactlyInAnyOrder(new Item("A", 3), new Item("Z", 1), new Item("B", 2));
    }
}
