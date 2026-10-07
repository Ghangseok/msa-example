package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 9.1의 재고 쪽: 예약한 재고를 해제하면 수량이 돌아온다.
 * 이 테스트 케이스의 비고가 "재고 쪽(해제하면 수량이 복구된다)은 재고 서비스 테스트로 검증한다"고 나눠 두었다.
 */
class ReleaseRestoresStockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-004 30초 안에 예약 결과를 못 받으면 실패로 기록하고 재고를 해제한다: 재고 쪽은 해제하면 수량이 복구된다")
    void releaseRestoresReservedStock() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);
        // 응답을 못 받은 주문 번호 하나로 상품 A 3개를 예약한다. 문서에 주문 번호가 없다. 테스트가 정한 값이다.
        assertThat(reserve(4001, new Item("A", 3)).result()).isEqualTo("RESERVED");

        // 같은 주문 번호로 해제한다
        Reply released = release(4001);

        assertThat(released.result()).isEqualTo("RELEASED");
        // And 상품 A의 재고는 10개다
        assertThat(stockOf("A")).isEqualTo(10);
    }
}
