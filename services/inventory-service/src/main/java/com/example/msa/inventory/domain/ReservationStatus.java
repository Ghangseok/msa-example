package com.example.msa.inventory.domain;

/**
 * 예약 기록의 상태다. 값 목록은 contracts/inventory-api.yaml의 ReservationResult.status와 같다.
 * 값을 나중에 더하면 계약을 깨는 변경이다.
 */
public enum ReservationStatus {
    /** 예약됨. 항목마다 수량을 줄였다. */
    RESERVED,
    /** 거절됨. 재고가 부족하거나 없는 상품이 있어 아무 수량도 줄이지 않았다. */
    REJECTED,
    /** 해제됨. 예약을 해제해 수량을 되돌렸거나, 예약보다 해제가 먼저 와서 해제 표식만 남겼다. */
    RELEASED
}
