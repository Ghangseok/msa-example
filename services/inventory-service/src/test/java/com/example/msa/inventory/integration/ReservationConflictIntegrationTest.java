package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 1.4: 같은 주문 번호에 다른 내용이 오면 거부한다.
 * 이 스토리에서 검증하는 줄은 Given, When, "요청 키 충돌(409)", "재고는 변하지 않는다" 네 줄이다.
 * 주문 서비스 쪽 두 줄(재시도하지 않는다, "실패"로 기록하고 해제 요청이 간다)은 주문 서비스 테스트가 검증한다.
 */
class ReservationConflictIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-008 같은 주문 번호에 다른 내용이 오면 거부한다")
    void differentContentForSameOrderNumberIsRejected() {
        // 문서에 초기 재고가 없다. 테스트가 정한 값(10개)이다.
        insertStock("A", 10);
        // Given 주문 번호 1003으로 상품 A 3개 예약이 처리됐다
        assertThat(reserve(1003, new Item("A", 3)).result()).isEqualTo("RESERVED");
        int stockBefore = stockOf("A");

        // When 주문 번호 1003, 상품 A, 5개로 예약을 요청한다
        Reply conflict = reserve(1003, new Item("A", 5));

        // Then 재고 서비스는 요청 키 충돌(409)을 돌려준다
        assertThat(conflict.status()).isEqualTo(409);
        // And 재고는 변하지 않는다
        assertThat(stockOf("A")).isEqualTo(stockBefore);
    }
}
