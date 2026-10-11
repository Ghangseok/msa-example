package com.example.msa.order.domain;

/**
 * 거절이나 실패의 사유다. 처리중과 확정에서는 사유가 없다. DB와 API에는 이름 문자열로 담는다.
 * 값 목록은 contracts/order-api.yaml의 OrderResponse.reason과 같다.
 */
public enum OrderReason {
    /** 재고 부족(거절). */
    OUT_OF_STOCK,
    /** 재고 데이터에 없는 상품(거절). */
    PRODUCT_NOT_FOUND,
    /** 잠시 후 다시 시도(실패). 다시 주문하려면 새 요청 키를 쓴다. */
    RETRY_LATER
}
