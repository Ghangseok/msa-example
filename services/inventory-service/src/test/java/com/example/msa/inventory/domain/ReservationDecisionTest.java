package com.example.msa.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.inventory.domain.ReservationDecision.ExistingJudgement;
import com.example.msa.inventory.dto.ReservationWithItems;
import com.example.msa.inventory.dto.ReserveRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 같은 주문 번호의 예약 기록이 이미 있을 때의 판단을 본다. Spring 없이 돈다.
 *
 * <p>기대값의 출처는 도메인 분석 6절이다. 첫 문단("내용(상품 순으로 정렬한 항목 목록)이 다르면 호출한 쪽의 버그로 본다")과
 * 표의 1번(같은 내용이면 저장된 첫 결과를 그대로 돌려준다), 3번(다른 내용이면 "요청 키 충돌"), 7번(해제 표식이 있으면 반영하지 않고 "해제됨"을 돌려준다)이다.
 * 값은 시나리오 1.1(주문 번호 1001, 상품 A 3개), 1.3(주문 번호 1002), 1.4(상품 A 5개)와 1.5(항목 순서)의 값을 쓴다.
 */
class ReservationDecisionTest {

    private static ReserveRequest.Item item(String productId, int quantity) {
        return new ReserveRequest.Item(productId, quantity);
    }

    private static ReservationWithItems.StoredItem stored(String productId, int quantity) {
        return new ReservationWithItems.StoredItem(productId, quantity, null);
    }

    private static ReservationWithItems reservation(
            long orderNo, ReservationStatus status, ReservationWithItems.StoredItem... items) {
        ReservationWithItems reservation = new ReservationWithItems();
        reservation.setOrderNo(orderNo);
        reservation.setStatus(status);
        reservation.setItems(List.of(items));
        return reservation;
    }

    @Test
    @DisplayName("같은 내용(저장된 A 3개, 들어온 A 3개)이면 저장된 결과를 돌려준다고 판단한다")
    void sameContentReturnsStoredResult() {
        ReservationWithItems stored = reservation(1001, ReservationStatus.RESERVED, stored("A", 3));

        ExistingJudgement judgement = ReservationDecision.judgeExisting(stored, List.of(item("A", 3)));

        assertThat(judgement).isEqualTo(ExistingJudgement.STORED_RESULT);
    }

    @Test
    @DisplayName("다른 내용(저장된 A 3개, 들어온 A 5개)이면 충돌이라고 판단한다")
    void differentContentIsConflict() {
        ReservationWithItems stored = reservation(1003, ReservationStatus.RESERVED, stored("A", 3));

        ExistingJudgement judgement = ReservationDecision.judgeExisting(stored, List.of(item("A", 5)));

        assertThat(judgement).isEqualTo(ExistingJudgement.CONFLICT);
    }

    @Test
    @DisplayName("항목 순서만 다르면 같은 내용이라고 판단한다")
    void onlyItemOrderDiffersIsSameContent() {
        ReservationWithItems stored = reservation(1001, ReservationStatus.RESERVED, stored("A", 1), stored("B", 1));

        ExistingJudgement judgement = ReservationDecision.judgeExisting(stored, List.of(item("B", 1), item("A", 1)));

        assertThat(judgement).isEqualTo(ExistingJudgement.STORED_RESULT);
    }

    @Test
    @DisplayName("해제 표식(해제됨, 항목 없음)이면 내용을 비교하지 않고 해제됨이라고 판단한다")
    void releaseMarkerIsNotCompared() {
        ReservationWithItems marker = reservation(1002, ReservationStatus.RELEASED);

        ExistingJudgement judgement = ReservationDecision.judgeExisting(marker, List.of(item("A", 3)));

        assertThat(judgement).isEqualTo(ExistingJudgement.RELEASE_MARKER);
    }
}
