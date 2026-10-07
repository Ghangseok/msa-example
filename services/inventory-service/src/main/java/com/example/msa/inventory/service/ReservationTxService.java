package com.example.msa.inventory.service;

import com.example.msa.inventory.domain.ReservationDecision;
import com.example.msa.inventory.domain.ReservationReason;
import com.example.msa.inventory.domain.ReservationStatus;
import com.example.msa.inventory.dto.ReleaseResponse;
import com.example.msa.inventory.dto.ReservationResponse;
import com.example.msa.inventory.dto.ReservationWithItems;
import com.example.msa.inventory.dto.ReserveRequest;
import com.example.msa.inventory.dto.StockRow;
import com.example.msa.inventory.exception.ReservationConflictException;
import com.example.msa.inventory.mapper.ReservationMapper;
import com.example.msa.inventory.mapper.StockMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약과 해제를 각각 한 트랜잭션으로 처리한다. {@code @Transactional} 메서드만 모아 둔 별도 빈이다.
 * 같은 클래스 안에서 부르면 Spring 프록시를 거치지 않아 트랜잭션이 걸리지 않기 때문이다.
 * 업무 판단은 {@link ReservationDecision}에 있고, 이 클래스는 순서를 조립한다.
 * 근거는 설계 문서 6절 "예약 처리"와 "해제 처리"다.
 */
@Service
public class ReservationTxService {

    private final StockMapper stockMapper;
    private final ReservationMapper reservationMapper;

    public ReservationTxService(StockMapper stockMapper, ReservationMapper reservationMapper) {
        this.stockMapper = stockMapper;
        this.reservationMapper = reservationMapper;
    }

    /**
     * 예약한다. 같은 주문 번호가 동시에 들어와 예약 기록의 기본 키에 걸리면 DuplicateKeyException이 나고, 이 트랜잭션은 모두 되돌려진다.
     * 그 경우 다시 하는 일은 부르는 쪽({@link ReservationService})이 한다.
     */
    @Transactional
    public ReservationResponse reserve(long orderNo, List<ReserveRequest.Item> items) {
        List<ReserveRequest.Item> sorted = ReservationDecision.sortByProductId(items);

        // 1. 같은 주문 번호의 예약 기록이 있으면, 내용을 비교해 저장된 결과를 돌려주거나 충돌로 거부한다.
        ReservationWithItems stored = reservationMapper.selectReservationWithItems(orderNo);
        if (stored != null) {
            return respondToExisting(orderNo, stored, sorted);
        }

        // 2. 항목의 재고 행을 상품 ID 순서로 하나씩 잠그고 읽는다. 재고 데이터에 없는 상품은 맵에 넣지 않는다.
        Map<String, Integer> stockByProductId = new HashMap<>();
        for (ReserveRequest.Item item : sorted) {
            StockRow row = stockMapper.selectStockForUpdate(item.productId());
            if (row != null) {
                stockByProductId.put(item.productId(), row.quantity());
            }
        }

        // 3. 잠금을 먼저 다 잡고 판단한다. 거절이면 아무 수량도 바꾸지 않고, 예약됨이면 항목마다 수량을 줄인다.
        ReservationDecision decision = ReservationDecision.decide(sorted, stockByProductId);
        if (decision.status() == ReservationStatus.RESERVED) {
            for (ReserveRequest.Item item : sorted) {
                stockMapper.updateDecreaseQuantity(item.productId(), item.quantity());
            }
        }

        // 4. 예약 기록과 항목(항목별 결과 포함)을 넣는다.
        reservationMapper.insertReservation(orderNo, decision.status(), decision.reason());
        for (ReserveRequest.Item item : sorted) {
            reservationMapper.insertReservationItem(
                    orderNo, item.productId(), item.quantity(), decision.resultOf(item.productId()));
        }
        return new ReservationResponse(
                orderNo,
                decision.status(),
                decision.reason(),
                decision.shortageProductIds(),
                decision.missingProductIds());
    }

    /**
     * 예약을 해제한다. 예약됨이면 수량을 되돌리고 해제됨으로 바꾼다. 거절됨이나 해제됨이면 그대로 둔다.
     * 예약 기록이 없으면 해제 표식을 넣는다. 늦게 온 예약 요청이 재고를 줄이지 못하게 막는다.
     * 해제 표식을 넣다 기본 키에 걸리면 DuplicateKeyException이 나고, 다시 하는 일은 부르는 쪽이 한다.
     */
    @Transactional
    public ReleaseResponse release(long orderNo) {
        // 1. 예약 기록 한 줄을 잠그고 읽는다.
        ReservationStatus status = reservationMapper.selectStatusForUpdate(orderNo);
        if (status == null) {
            reservationMapper.insertReservation(orderNo, ReservationStatus.RELEASED, null);
            return new ReleaseResponse(orderNo, ReservationStatus.RELEASED);
        }
        if (status == ReservationStatus.RESERVED) {
            // 2. 항목을 상품 ID 순서로 읽어 수량을 되돌린다.
            ReservationWithItems stored = reservationMapper.selectReservationWithItems(orderNo);
            for (ReservationWithItems.StoredItem item : stored.getItems()) {
                stockMapper.updateIncreaseQuantity(item.productId(), item.quantity());
            }
            reservationMapper.updateReservationStatus(orderNo, ReservationStatus.RELEASED);
            return new ReleaseResponse(orderNo, ReservationStatus.RELEASED);
        }
        return new ReleaseResponse(orderNo, status);
    }

    private ReservationResponse respondToExisting(
            long orderNo, ReservationWithItems stored, List<ReserveRequest.Item> sortedItems) {
        return switch (ReservationDecision.judgeExisting(stored, sortedItems)) {
            case CONFLICT -> throw new ReservationConflictException(orderNo);
            case RELEASE_MARKER ->
                new ReservationResponse(orderNo, ReservationStatus.RELEASED, null, List.of(), List.of());
            case STORED_RESULT -> toResponse(stored);
        };
    }

    /** 저장된 예약 기록에서 처음과 같은 응답을 다시 만든다. 부족한 상품과 없는 상품 목록은 항목별 결과에서 만든다. */
    private static ReservationResponse toResponse(ReservationWithItems stored) {
        List<String> shortage = stored.getItems().stream()
                .filter(item -> item.result() == ReservationReason.OUT_OF_STOCK)
                .map(ReservationWithItems.StoredItem::productId)
                .toList();
        List<String> missing = stored.getItems().stream()
                .filter(item -> item.result() == ReservationReason.PRODUCT_NOT_FOUND)
                .map(ReservationWithItems.StoredItem::productId)
                .toList();
        return new ReservationResponse(stored.getOrderNo(), stored.getStatus(), stored.getReason(), shortage, missing);
    }
}
