package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 1.8: 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다. 기대값은 docs/test-cases/order-placement.md에서 가져온다. */
class ReleaseTwiceIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-020 예약된 재고에 해제가 두 번 와도 수량은 한 번만 돌아온다")
    void releaseTwiceRestoresStockOnce() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);
        // And 주문 번호 2003으로 상품 A 3개 예약이 처리됐다
        assertThat(reserve(2003, new Item("A", 3)).result()).isEqualTo("RESERVED");

        // When 주문 번호 2003의 해제 요청이 온다
        Reply first = release(2003);

        // Then 해제 요청은 RELEASED를 돌려준다
        assertThat(first.result()).isEqualTo("RELEASED");
        // And 상품 A의 재고는 10개다
        assertThat(stockOf("A")).isEqualTo(10);

        // When 같은 해제 요청이 한 번 더 온다
        Reply second = release(2003);

        // Then 해제 요청은 RELEASED를 돌려준다
        assertThat(second.result()).isEqualTo("RELEASED");
        // And 상품 A의 재고는 여전히 10개다 (13개가 아니다)
        assertThat(stockOf("A")).isEqualTo(10);
    }
}
