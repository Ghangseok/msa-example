package com.example.msa.order.client;

import com.example.msa.order.domain.OrderReason;
import java.util.List;

/**
 * 재고 서비스의 예약 결과다. contracts/inventory-api.yaml의 ReservationResult와 같다.
 * 받는 쪽은 모르는 필드를 무시한다.
 *
 * @param reason 거절일 때만 값이 있다(재고 부족이나 상품 없음). 그 밖에는 null이다
 * @param shortageProductIds 재고가 부족한 상품 ID. 상품 ID 순
 * @param missingProductIds 재고 데이터에 없는 상품 ID. 상품 ID 순
 */
public record ReservationResult(
        long orderNo,
        Status status,
        OrderReason reason,
        List<String> shortageProductIds,
        List<String> missingProductIds) {

    /** 예약 결과 상태다. 값 목록은 재고 API 계약과 같다. */
    public enum Status {
        RESERVED,
        REJECTED,
        RELEASED
    }
}
