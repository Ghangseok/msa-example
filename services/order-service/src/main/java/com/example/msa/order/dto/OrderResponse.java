package com.example.msa.order.dto;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import java.util.List;

/**
 * 주문 결과 응답이다. contracts/order-api.yaml의 OrderResponse와 같다. 해제 결과(releaseStatus)는 담지 않는다.
 *
 * @param reason 거절이면 재고 부족이나 상품 없음, 실패이면 잠시 후 다시 시도다. 그 밖에는 null이다
 * @param shortageProductIds 재고가 부족한 상품 ID. 상품 ID 순. 없으면 빈 목록
 * @param missingProductIds 재고 데이터에 없는 상품 ID. 상품 ID 순. 없으면 빈 목록
 */
public record OrderResponse(
        long orderNo,
        OrderStatus status,
        OrderReason reason,
        List<String> shortageProductIds,
        List<String> missingProductIds) {

    /** 저장된 주문에서 응답을 만든다. 부족한 상품과 없는 상품 목록은 항목별 거절 사유에서 만든다. */
    public static OrderResponse from(OrderWithItems order) {
        List<String> shortage = order.getItems().stream()
                .filter(item -> item.rejectReason() == OrderReason.OUT_OF_STOCK)
                .map(OrderWithItems.StoredItem::productId)
                .toList();
        List<String> missing = order.getItems().stream()
                .filter(item -> item.rejectReason() == OrderReason.PRODUCT_NOT_FOUND)
                .map(OrderWithItems.StoredItem::productId)
                .toList();
        return new OrderResponse(order.getOrderNo(), order.getStatus(), order.getReason(), shortage, missing);
    }
}
