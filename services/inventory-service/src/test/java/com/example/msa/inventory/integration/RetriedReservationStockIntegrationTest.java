package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 8.1의 재고 쪽: 재시도로 같은 예약 요청이 여러 번 와도 재고는 한 번만 줄어든다.
 * 주문 서비스가 재시도로 같은 요청을 세 번 보내는 상황과 같게, 같은 예약 요청을 세 번 보낸다.
 */
class RetriedReservationStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-006 재고 서비스의 일시 오류는 재시도로 넘어간다: 재고 쪽은 같은 예약 요청이 세 번 와도 7개다")
    void sameReservationSentThreeTimesReducesStockOnce() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);

        // 같은 주문 번호, 같은 내용(상품 A 3개)의 예약 요청을 세 번 보낸다. 주문 번호는 테스트가 정한 값이다.
        assertThat(reserve(8001, new Item("A", 3)).result()).isEqualTo("RESERVED");
        assertThat(reserve(8001, new Item("A", 3)).result()).isEqualTo("RESERVED");
        assertThat(reserve(8001, new Item("A", 3)).result()).isEqualTo("RESERVED");

        // And 상품 A의 재고는 7개다
        assertThat(stockOf("A")).isEqualTo(7);
    }
}
