package com.example.msa.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 주문 상태 전이의 단위 테스트다. Spring도 DB도 쓰지 않는다.
 *
 * <p>기대값의 출처는 도메인 분석 4절 "주문 상태"의 그림이다. 그림에 있는 전이만 허락하고 나머지는 막는다.
 * 그림에 있는 전이는 네 개다. 처리중에서 확정, 거절, 실패로 가고, 실패에서 실패로 간다(해제 결과를 주문에 기록할 때).
 */
class OrderStatusTest {

    @Test
    @DisplayName("처리중에서는 확정, 거절, 실패로 갈 수 있다")
    void pendingMovesToConfirmedRejectedOrFailed() {
        assertThat(OrderStatus.PENDING.canMoveTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PENDING.canMoveTo(OrderStatus.REJECTED)).isTrue();
        assertThat(OrderStatus.PENDING.canMoveTo(OrderStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("처리중에서 처리중으로는 가지 않는다")
    void pendingDoesNotMoveToPending() {
        assertThat(OrderStatus.PENDING.canMoveTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    @DisplayName("확정에서는 어느 상태로도 가지 않는다")
    void confirmedMovesNowhere() {
        assertThat(movesFrom(OrderStatus.CONFIRMED)).isEmpty();
    }

    @Test
    @DisplayName("거절에서는 어느 상태로도 가지 않는다")
    void rejectedMovesNowhere() {
        assertThat(movesFrom(OrderStatus.REJECTED)).isEmpty();
    }

    @Test
    @DisplayName("실패에서는 실패로만 간다(해제 결과를 기록할 때)")
    void failedMovesOnlyToFailed() {
        assertThat(movesFrom(OrderStatus.FAILED)).containsExactly(OrderStatus.FAILED);
    }

    private static Set<OrderStatus> movesFrom(OrderStatus from) {
        Set<OrderStatus> moves = EnumSet.noneOf(OrderStatus.class);
        for (OrderStatus to : OrderStatus.values()) {
            if (from.canMoveTo(to)) {
                moves.add(to);
            }
        }
        return moves;
    }
}
