package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 1.3: 해제가 예약보다 먼저 도착해도 재고는 줄지 않는다. 기대값은 docs/test-cases/order-placement.md에서 가져온다. */
class ReleaseBeforeReserveIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-007 해제가 예약보다 먼저 도착해도 재고는 줄지 않는다")
    void releaseArrivingBeforeReserveKeepsStock() {
        // Given 상품 A의 재고가 10개이고, 주문 번호 1002의 예약 기록이 없다
        insertStock("A", 10);

        // When 주문 번호 1002의 해제 요청이 먼저 오고, 그 뒤에 1002, 상품 A, 3개의 예약 요청이 도착한다
        Reply released = release(1002);
        Reply reserved = reserve(1002, new Item("A", 3));

        // Then 해제 요청은 RELEASED를 돌려준다
        assertThat(released.result()).isEqualTo("RELEASED");
        // And 예약 요청은 반영되지 않고 RELEASED를 돌려준다
        assertThat(reserved.result()).isEqualTo("RELEASED");
        // And 상품 A의 재고는 10개다
        assertThat(stockOf("A")).isEqualTo(10);

        // When 같은 해제 요청이 한 번 더 온다
        release(1002);

        // Then 재고는 여전히 10개다
        assertThat(stockOf("A")).isEqualTo(10);
    }
}
