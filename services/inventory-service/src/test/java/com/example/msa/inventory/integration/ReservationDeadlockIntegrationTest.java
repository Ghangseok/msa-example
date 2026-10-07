package com.example.msa.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 1.5: 여러 상품 주문이 서로 다른 순서로 동시에 들어와도 교착 없이 처리된다.
 * 교착이 나면 재고 서비스는 500을 돌려준다. 그래서 교착이 한 번이라도 나면 "100건 모두 확정"이 깨진다.
 */
class ReservationDeadlockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("TC-012 여러 상품 주문이 서로 다른 순서로 동시에 들어와도 교착 없이 처리된다")
    void ordersWithOppositeItemOrdersDoNotDeadlock() {
        // Given 상품 A와 B의 재고가 각각 100개다
        insertStock("A", 100);
        insertStock("B", 100);

        // When 주문 50건은 "B 1개, A 1개" 순서로, 다른 50건은 "A 1개, B 1개" 순서로 항목을 적어 동시에 요청한다
        // 두 순서가 섞여 부딪히도록 번갈아 담는다.
        List<Callable<Reply>> requests = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long bThenA = 12001L + i;
            long aThenB = 12101L + i;
            requests.add(() -> reserve(bThenA, new Item("B", 1), new Item("A", 1)));
            requests.add(() -> reserve(aThenB, new Item("A", 1), new Item("B", 1)));
        }
        List<Reply> replies = runConcurrently(requests);

        // Then 100건 모두 "확정"이다. 교착 오류(ORA-00060)로 실패한 주문이 없다
        assertThat(replies).hasSize(100).extracting(Reply::result).containsOnly("RESERVED");
        // And 상품 A와 B의 재고는 각각 0개다
        assertThat(stockOf("A")).isZero();
        assertThat(stockOf("B")).isZero();
    }
}
