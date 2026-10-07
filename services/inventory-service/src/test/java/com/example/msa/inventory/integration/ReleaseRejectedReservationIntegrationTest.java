package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 1.6: 거절된 예약에 해제 요청이 와도 재고는 그대로다. 기대값은 docs/test-cases/order-placement.md에서 가져온다. */
class ReleaseRejectedReservationIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-016 거절된 예약에 해제 요청이 와도 재고는 그대로다")
    void releaseOfRejectedReservationKeepsStock() {
        // Given 상품 A의 재고가 2개다
        insertStock("A", 2);
        // And 주문 번호 2001로 상품 A 3개 예약을 요청해 REJECTED를 받았다
        assertThat(reserve(2001, new Item("A", 3)).result()).isEqualTo("REJECTED");

        // When 주문 번호 2001의 해제 요청이 온다
        Reply released = release(2001);

        // Then 해제 요청은 REJECTED를 돌려준다
        assertThat(released.result()).isEqualTo("REJECTED");
        // And 상품 A의 재고는 2개다
        assertThat(stockOf("A")).isEqualTo(2);
    }
}
