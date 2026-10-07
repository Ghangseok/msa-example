package com.example.msa.inventory.domain;

/**
 * 거절 사유다. 값 목록은 contracts/inventory-api.yaml의 ReservationResult.reason과 같다.
 * 값을 나중에 더하면 계약을 깨는 변경이다.
 */
public enum ReservationReason {
    /** 재고가 부족하다. */
    OUT_OF_STOCK,
    /** 재고 데이터에 없는 상품이다. 재고 부족보다 먼저 정한다. */
    PRODUCT_NOT_FOUND
}
