package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 1.7: 해제한 예약의 기록은 남아 있어서, 같은 예약 요청이 다시 와도 재고가 줄지 않는다. 기대값은 docs/test-cases/order-placement.md에서 가져온다. */
class ReserveAfterReleaseIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-017 해제한 예약의 기록은 남아 있어서, 같은 예약 요청이 다시 와도 재고가 줄지 않는다")
    void reserveAfterReleaseDoesNotReduceStock() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);
        // And 주문 번호 2002로 상품 A 3개 예약이 처리됐고, 그 뒤 해제됐다
        assertThat(reserve(2002, new Item("A", 3)).result()).isEqualTo("RESERVED");
        assertThat(release(2002).result()).isEqualTo("RELEASED");

        // When 주문 번호 2002, 상품 A, 3개로 예약을 다시 요청한다
        Reply again = reserve(2002, new Item("A", 3));

        // Then 재고 서비스는 RELEASED를 돌려준다
        assertThat(again.result()).isEqualTo("RELEASED");
        // And 상품 A의 재고는 10개다
        assertThat(stockOf("A")).isEqualTo(10);
    }
}
