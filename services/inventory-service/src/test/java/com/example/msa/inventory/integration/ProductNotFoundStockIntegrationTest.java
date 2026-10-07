package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 3.3의 재고 쪽: 없는 상품이 섞이면 예약 전체가 거절된다.
 * 같은 예약 요청을 재고 서비스에 보낸 뒤 응답의 사유와 두 목록, 재고를 검사한다.
 * 사유 "상품 없음"은 PRODUCT_NOT_FOUND, "거절"은 REJECTED다.
 */
class ProductNotFoundStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-015 없는 상품이 섞이면 주문 전체가 거절되고 기록된다: 재고 쪽 본 흐름")
    void unknownProductRejectsWholeReservation() {
        // Given 상품 A의 재고가 10개다. 상품 Z는 재고 데이터에 없다
        insertStock("A", 10);

        // When 상품 A 3개와 상품 Z 1개를 한 예약으로 요청한다. 주문 번호는 테스트가 정한 값이다.
        Reply reply = reserve(3, new Item("A", 3), new Item("Z", 1));

        // Then 예약 결과는 REJECTED("거절")이고, 사유는 "상품 없음", 없는 상품은 Z다
        assertThat(reply.result()).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reply.texts("$.missingProductIds")).isEqualTo(List.of("Z"));
        // And 상품 A의 재고는 10개 그대로다
        assertThat(stockOf("A")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-015 변형: 없는 상품과 부족한 상품이 함께 있으면 사유는 상품 없음이고 두 목록을 모두 받는다")
    void unknownAndShortProductsAreBothReported() {
        // 상품 B의 재고가 1개일 때
        insertStock("A", 10);
        insertStock("B", 1);

        // 상품 A 3개, 상품 Z 1개, 상품 B 2개를 한 예약으로 요청하면
        Reply reply = reserve(4, new Item("A", 3), new Item("Z", 1), new Item("B", 2));

        // 사유는 "상품 없음"이고 없는 상품 Z와 부족한 상품 B를 모두 받는다
        assertThat(reply.result()).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reply.texts("$.missingProductIds")).isEqualTo(List.of("Z"));
        assertThat(reply.texts("$.shortageProductIds")).isEqualTo(List.of("B"));
        // 상품 A와 B의 재고는 그대로다
        assertThat(stockOf("A")).isEqualTo(10);
        assertThat(stockOf("B")).isEqualTo(1);
    }
}
