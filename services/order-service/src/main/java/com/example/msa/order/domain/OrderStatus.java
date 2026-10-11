package com.example.msa.order.domain;

/**
 * 주문 상태다. 처리중, 확정, 거절, 실패 네 가지이고 DB와 API에는 이름 문자열로 담는다.
 * 어느 상태에서 어느 상태로 갈 수 있는지는 {@link #canMoveTo(OrderStatus)}가 정한다.
 * 근거는 도메인 분석 4절 "주문 상태"의 그림과 data-model.md 4-1절이다.
 */
public enum OrderStatus {
    /** 처리중. 주문을 기록했고 재고 예약 결과를 아직 모른다. */
    PENDING,
    /** 확정. 모든 항목의 재고를 예약했다. */
    CONFIRMED,
    /** 거절. 재고가 부족하거나 없는 상품이 있다. */
    REJECTED,
    /** 실패. 예약 결과를 얻지 못했다(서킷이 열림, 409·500, 일시 오류). */
    FAILED;

    /**
     * 이 상태에서 {@code next} 상태로 갈 수 있는지 알려 준다.
     * 처리중에서는 확정, 거절, 실패로 갈 수 있다. 확정과 거절에서는 어디로도 가지 않는다.
     * 실패에서는 실패로만 간다. 재고 해제 결과를 주문에 기록하는 것이 이 경우다.
     */
    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == REJECTED || next == FAILED;
            case FAILED -> next == FAILED;
            case CONFIRMED, REJECTED -> false;
        };
    }
}
