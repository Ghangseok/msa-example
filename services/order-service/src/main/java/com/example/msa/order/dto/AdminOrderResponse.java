package com.example.msa.order.dto;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 조회의 주문 하나다. specs/002-view-orders/contracts/order-query-api.yaml의 AdminOrder와 같다.
 * {@link OrderDetailResponse}의 모든 필드에 재고 해제 결과가 더해진 모양이다. 주문한 고객의 ID는 담지 않는다.
 *
 * @param releaseStatus 재고 해제 결과. 해제 요청 전이거나 "실패"가 아니면 null이다. 필드는 항상 있다
 */
public record AdminOrderResponse(
        long orderNo,
        OrderStatus status,
        OrderReason reason,
        List<String> shortageProductIds,
        List<String> missingProductIds,
        List<OrderDetailResponse.Line> items,
        LocalDateTime createdAt,
        ReleaseStatus releaseStatus) {

    /** 저장된 주문에서 응답을 만든다. 고객용 응답을 먼저 만들고 해제 결과를 더한다. */
    public static AdminOrderResponse from(OrderWithItems order) {
        OrderDetailResponse detail = OrderDetailResponse.from(order);
        return new AdminOrderResponse(
                detail.orderNo(),
                detail.status(),
                detail.reason(),
                detail.shortageProductIds(),
                detail.missingProductIds(),
                detail.items(),
                detail.createdAt(),
                order.getReleaseStatus());
    }
}
