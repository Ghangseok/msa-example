package com.example.msa.order.service;

import com.example.msa.order.client.InventoryCallContext;
import com.example.msa.order.client.InventoryCallFailure;
import com.example.msa.order.client.InventoryClient;
import com.example.msa.order.client.ReservationResult;
import com.example.msa.order.domain.OrderItems;
import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.dto.OrderItemRequest;
import com.example.msa.order.dto.OrderResponse;
import com.example.msa.order.dto.OrderWithItems;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 주문 흐름을 조립한다. 이 클래스에는 트랜잭션이 없다. 저장은 {@link OrderTxService}가 메서드마다 따로 커밋하고,
 * 재고 호출은 그 사이에서 트랜잭션 밖에 한다. 원격 호출을 기다리는 동안 DB 연결과 잠금을 잡고 있지 않게 하려는 것이다.
 * 근거는 data-model.md 6-1절과 docs/standards/architecture-rules.md 2절 "서비스 사이 호출"이다.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderTxService orderTxService;
    private final InventoryClient inventoryClient;
    private final ReservationReleaser reservationReleaser;

    public OrderService(
            OrderTxService orderTxService, InventoryClient inventoryClient, ReservationReleaser reservationReleaser) {
        this.orderTxService = orderTxService;
        this.inventoryClient = inventoryClient;
        this.reservationReleaser = reservationReleaser;
    }

    /** 주문 요청의 처리 결과다. {@code outcome}이 새로 기록했는지, 이미 있는 주문인지, 키 충돌인지 알려 준다. */
    public record PlaceResult(OrderResponse order, Outcome outcome) {

        /** 처리 결과의 종류다. */
        public enum Outcome {
            /** 새 주문을 기록했다(201). */
            CREATED,
            /** 같은 고객이 같은 키로 같은 내용을 다시 보냈다. 이미 있는 주문의 지금 상태다(200). */
            EXISTING,
            /** 같은 키인데 내용이 다르다(422). 주문을 만들지 않았다. */
            CONFLICT
        }
    }

    /**
     * 주문을 "처리중"으로 저장하고, 재고를 예약하고, 결과를 저장한다.
     * 같은 고객의 같은 요청 키가 이미 있으면 새 주문을 만들지 않고 재고 서비스도 부르지 않는다. 내용이 같으면 이미 있는 주문의
     * 지금 상태를, 다르면 충돌을 돌려준다.
     *
     * <p>재고 서비스가 업무 결과를 돌려주지 못하면(409, 500, 연결 실패, 시간 초과, 서킷 열림) 주문을 "실패"로 저장한다.
     * 예약 요청을 보낸 적이 있으면 재고가 줄었을 수 있으므로, 고객에게 바로 응답한 뒤 해제를 따로 돌린다.
     */
    public PlaceResult place(String customerId, String requestKey, List<OrderItemRequest> items) {
        OrderWithItems pending;
        try {
            pending = orderTxService.savePending(customerId, requestKey, items);
        } catch (DuplicateKeyException e) {
            return answerForExistingKey(customerId, requestKey, items);
        }
        long orderNo = pending.getOrderNo();
        InventoryCallContext context = inventoryClient.newCallContext();
        ReservationResult result = null;
        try {
            result = inventoryClient.reserve(orderNo, items, context);
        } catch (InventoryCallFailure failure) {
            log.warn(
                    "재고 예약 결과를 얻지 못했다. 주문을 실패로 기록한다. orderNo={} kind={} httpStatus={}",
                    orderNo,
                    failure.kind(),
                    failure.httpStatus());
        }
        OrderWithItems applied = applyResult(orderNo, result, context.requestWasSent());
        if (applied.getStatus() == OrderStatus.FAILED && context.requestWasSent()) {
            reservationReleaser.releaseLater(orderNo);
        }
        return new PlaceResult(OrderResponse.from(applied), PlaceResult.Outcome.CREATED);
    }

    /**
     * 재고 서비스의 예약 결과를 주문 상태로 바꿔 저장한다. 예약됨이면 확정, 거절됨이면 거절(사유와 상품 목록 포함)이다.
     * 결과를 얻지 못했거나({@code result}가 null) 해제됨(RELEASED)을 받았으면 실패(RETRY_LATER)다.
     * 해제됨은 일어나지 않아야 하지만 받으면 실패로 둔다 [제안].
     */
    private OrderWithItems applyResult(long orderNo, ReservationResult result, boolean reservationRequestSent) {
        if (result != null && result.status() == ReservationResult.Status.RESERVED) {
            return orderTxService.applyResult(orderNo, OrderStatus.CONFIRMED, null, List.of(), List.of(), true);
        }
        if (result != null && result.status() == ReservationResult.Status.REJECTED) {
            return orderTxService.applyResult(
                    orderNo,
                    OrderStatus.REJECTED,
                    result.reason(),
                    result.shortageProductIds(),
                    result.missingProductIds(),
                    true);
        }
        return orderTxService.applyResult(
                orderNo, OrderStatus.FAILED, OrderReason.RETRY_LATER, List.of(), List.of(), reservationRequestSent);
    }

    /** 고유 제약(고객, 요청 키)에 걸렸다. 이미 있는 주문을 읽어 같은 요청인지 본다. */
    private PlaceResult answerForExistingKey(String customerId, String requestKey, List<OrderItemRequest> items) {
        OrderWithItems existing = orderTxService.findByCustomerKey(customerId, requestKey);
        if (OrderItems.sameContent(items, existing.getItems())) {
            return new PlaceResult(OrderResponse.from(existing), PlaceResult.Outcome.EXISTING);
        }
        log.info("같은 요청 키에 다른 내용이 왔다. orderNo={}", existing.getOrderNo());
        return new PlaceResult(OrderResponse.from(existing), PlaceResult.Outcome.CONFLICT);
    }
}
