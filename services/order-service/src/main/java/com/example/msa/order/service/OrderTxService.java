package com.example.msa.order.service;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import com.example.msa.order.dto.OrderItemRequest;
import com.example.msa.order.dto.OrderWithItems;
import com.example.msa.order.mapper.OrderMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 저장을 메서드마다 한 트랜잭션으로 처리한다. {@code @Transactional} 메서드만 모아 둔 별도 빈이다.
 * 같은 클래스 안에서 부르면 Spring 프록시를 거치지 않아 트랜잭션이 걸리지 않기 때문이다.
 * 재고 호출은 이 클래스 밖({@link OrderService})에서, 트랜잭션 밖에서 한다. 근거는 data-model.md 6-1절이다.
 */
@Service
public class OrderTxService {

    private final OrderMapper orderMapper;

    public OrderTxService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /** 트랜잭션 1. 주문을 "처리중"으로, 항목과 함께 저장한다. 주문 번호는 시퀀스에서 받는다. */
    @Transactional
    public OrderWithItems savePending(String customerId, String requestKey, List<OrderItemRequest> items) {
        OrderWithItems order = new OrderWithItems();
        order.setCustomerId(customerId);
        order.setRequestKey(requestKey);
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insertOrder(order);
        for (OrderItemRequest item : items) {
            orderMapper.insertOrderItem(order.getOrderNo(), item.productId(), item.quantityAsInt());
        }
        return orderMapper.selectOrderWithItems(order.getOrderNo());
    }

    /**
     * 고객과 주문 요청 키로 이미 있는 주문을 항목과 함께 읽는다. 같은 키로 다시 온 요청을 알아볼 때 쓴다.
     * {@link #savePending}이 고유 제약에 걸려 롤백된 뒤, 새 트랜잭션에서 읽는다.
     */
    @Transactional(readOnly = true)
    public OrderWithItems findByCustomerKey(String customerId, String requestKey) {
        return orderMapper.selectOrderWithItemsByCustomerKey(customerId, requestKey);
    }

    /**
     * 트랜잭션 2. 예약 결과를 주문에 저장한다. 결과를 주문 상태로 바꾸는 일은 {@link OrderService}가 하고, 이 메서드는 정해진 값을 저장한다.
     * {@code @Transactional} 코드는 {@code client} 패키지와 닿지 않는다. 그래서 재고 서비스의 응답 객체를 받지 않는다.
     *
     * <ul>
     *   <li>확정(CONFIRMED)이면 상태만 바꾼다.
     *   <li>거절(REJECTED)이면 사유와 항목별 거절 사유(부족한 상품은 OUT_OF_STOCK, 없는 상품은 PRODUCT_NOT_FOUND)도 저장한다.
     *   <li>실패(FAILED)이면 사유(RETRY_LATER)를 저장한다. 예약 요청을 한 번도 보내지 않았으면({@code reservationRequestSent}가
     *       false) 해제할 것이 없으므로 해제 결과에 "해제 불필요"도 함께 저장한다.
     * </ul>
     *
     * 상태를 바꾸기 전에 {@link OrderStatus#canMoveTo(OrderStatus)}로 갈 수 있는 상태인지 확인한다.
     *
     * @param next 주문이 갈 상태. 확정, 거절, 실패 가운데 하나다
     * @param reason 거절이나 실패의 사유. 확정이면 null이다
     * @param shortageProductIds 재고가 부족한 상품 ID. 거절일 때만 값이 있다
     * @param missingProductIds 재고 데이터에 없는 상품 ID. 거절일 때만 값이 있다
     * @param reservationRequestSent 서킷이 허가해 예약 요청을 실제로 보낸 적이 있는가
     */
    @Transactional
    public OrderWithItems applyResult(
            long orderNo,
            OrderStatus next,
            OrderReason reason,
            List<String> shortageProductIds,
            List<String> missingProductIds,
            boolean reservationRequestSent) {
        OrderWithItems order = orderMapper.selectOrderWithItems(orderNo);
        for (String productId : shortageProductIds) {
            orderMapper.updateItemRejectReason(orderNo, productId, OrderReason.OUT_OF_STOCK);
        }
        for (String productId : missingProductIds) {
            orderMapper.updateItemRejectReason(orderNo, productId, OrderReason.PRODUCT_NOT_FOUND);
        }
        boolean noReleaseNeeded = next == OrderStatus.FAILED && !reservationRequestSent;
        moveTo(order, next, reason, noReleaseNeeded ? ReleaseStatus.NOT_REQUIRED : null);
        return orderMapper.selectOrderWithItems(orderNo);
    }

    /**
     * 트랜잭션 3. "실패" 주문의 재고 해제 결과를 새 트랜잭션으로 저장한다. 해제는 고객 응답 뒤에 따로 돌고, 그 끝에 부른다.
     * 상태는 실패 그대로다(실패에서 실패로 가는 전이).
     */
    @Transactional
    public void applyReleaseResult(long orderNo, ReleaseStatus releaseStatus) {
        OrderWithItems order = orderMapper.selectOrderWithItems(orderNo);
        moveTo(order, OrderStatus.FAILED, order.getReason(), releaseStatus);
    }

    private void moveTo(OrderWithItems order, OrderStatus next, OrderReason reason, ReleaseStatus releaseStatus) {
        if (!order.getStatus().canMoveTo(next)) {
            throw new IllegalStateException(
                    "주문 " + order.getOrderNo() + "은 " + order.getStatus() + "에서 " + next + "로 갈 수 없다");
        }
        orderMapper.updateOrderResult(order.getOrderNo(), next, reason, releaseStatus);
    }
}
