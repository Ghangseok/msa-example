package com.example.msa.inventory.exception;

/**
 * 같은 주문 번호에 다른 내용의 예약 요청이 왔다. 호출한 쪽의 버그로 보고 반영하지 않는다.
 * 409 Problem Details(code = RESERVATION_CONFLICT)로 바뀐다. 주문 서비스는 이 응답에 재시도하지 않는다.
 */
public class ReservationConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationConflictException(long orderNo) {
        super("주문 번호 " + orderNo + "에 이미 다른 내용의 예약이 있다");
    }
}
