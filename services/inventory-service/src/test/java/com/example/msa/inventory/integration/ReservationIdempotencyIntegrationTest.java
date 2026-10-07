package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 1.1: 같은 주문으로 예약을 다시 요청해도 재고는 한 번만 줄어든다. 기대값은 docs/test-cases/order-placement.md에서 가져온다. */
class ReservationIdempotencyIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-003 같은 주문으로 예약을 다시 요청해도 재고는 한 번만 줄어든다")
    void sameOrderReservedAgainReducesStockOnce() {
        // Given 상품 A의 재고가 10개다
        insertStock("A", 10);
        // And 주문 번호 1001로 상품 A 3개 예약이 이미 처리됐다
        assertThat(reserve(1001, new Item("A", 3)).result()).isEqualTo("RESERVED");

        // When 같은 주문 번호 1001, 상품 A, 3개로 예약을 다시 요청한다
        Reply again = reserve(1001, new Item("A", 3));

        // Then 재고 서비스는 처음과 같은 RESERVED를 돌려준다
        assertThat(again.result()).isEqualTo("RESERVED");
        // And 상품 A의 재고는 4개가 아니라 7개다
        assertThat(stockOf("A")).isEqualTo(7);
    }

    @Test
    @DisplayName("TC-003 변형: 같은 요청 두 개를 동시에 보내도 재고는 7개이고 두 응답 모두 RESERVED다")
    void sameRequestSentTwiceAtOnceReducesStockOnce() {
        insertStock("A", 10);

        List<Callable<Reply>> requests =
                List.of(() -> reserve(1001, new Item("A", 3)), () -> reserve(1001, new Item("A", 3)));
        List<Reply> replies = runConcurrently(requests);

        assertThat(replies).extracting(Reply::result).containsExactly("RESERVED", "RESERVED");
        assertThat(stockOf("A")).isEqualTo(7);
    }

    @Test
    @DisplayName("TC-003 변형: 첫 결과가 REJECTED였다면, 그 뒤 재고를 채운 다음 재요청해도 REJECTED를 돌려준다")
    void rejectedResultIsReturnedAgainEvenAfterStockIsRefilled() {
        // 첫 결과를 REJECTED로 만들려고 재고를 요청 수량보다 적게 둔다. 값은 테스트가 정한 것이다.
        insertStock("A", 2);
        assertThat(reserve(1001, new Item("A", 3)).result()).isEqualTo("REJECTED");

        // 재고를 채운 다음 같은 요청을 다시 보낸다
        updateStock("A", 10);
        Reply again = reserve(1001, new Item("A", 3));

        assertThat(again.result()).isEqualTo("REJECTED");
    }
}
