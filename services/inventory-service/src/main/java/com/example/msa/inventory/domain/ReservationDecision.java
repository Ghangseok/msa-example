package com.example.msa.inventory.domain;

import com.example.msa.inventory.dto.ReservationWithItems;
import com.example.msa.inventory.dto.ReserveRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 예약의 업무 판단이다. Spring과 MyBatis를 모르는 순수 Java라서 따로 단위 테스트를 할 수 있다.
 *
 * <p>두 가지를 판단한다.
 * <ol>
 *   <li>같은 주문 번호의 예약 기록이 이미 있을 때, 들어온 요청을 어떻게 다룰지({@link #judgeExisting})
 *   <li>새 예약이 예약됨인지 거절됨인지, 거절이면 사유와 상품 목록({@link #decide})
 * </ol>
 *
 * 근거는 도메인 분석 6절 "재고 예약의 멱등성"과 설계 문서 6절 "예약 처리"다.
 */
public final class ReservationDecision {

    /** 같은 주문 번호의 예약 기록이 이미 있을 때 들어온 요청을 다루는 방법이다. */
    public enum ExistingJudgement {
        /** 같은 내용이다. 저장된 첫 결과를 그대로 돌려준다. */
        STORED_RESULT,
        /** 다른 내용이다. 호출한 쪽의 버그라서 반영하지 않고 충돌 오류를 돌려준다. */
        CONFLICT,
        /** 해제 표식이다. 내용을 비교하지 않고 반영하지 않으며, 해제됨을 돌려준다. */
        RELEASE_MARKER
    }

    private static final Comparator<ReserveRequest.Item> BY_PRODUCT_ID =
            Comparator.comparing(ReserveRequest.Item::productId);

    private final ReservationStatus status;
    private final ReservationReason reason;
    private final List<String> shortageProductIds;
    private final List<String> missingProductIds;
    private final Map<String, ReservationReason> results;

    private ReservationDecision(
            ReservationStatus status,
            ReservationReason reason,
            List<String> shortageProductIds,
            List<String> missingProductIds,
            Map<String, ReservationReason> results) {
        this.status = status;
        this.reason = reason;
        this.shortageProductIds = shortageProductIds;
        this.missingProductIds = missingProductIds;
        this.results = results;
    }

    /**
     * 같은 주문 번호의 예약 기록이 있을 때 들어온 요청을 어떻게 다룰지 판단한다.
     * 해제 표식(해제됨이고 항목이 없는 기록)이면 내용을 비교하지 않는다.
     * 그 밖에는 저장된 항목과 들어온 항목을 상품 ID 순으로 정렬해 비교한다. 같으면 저장된 결과를, 다르면 충돌을 돌려준다.
     */
    public static ExistingJudgement judgeExisting(ReservationWithItems stored, List<ReserveRequest.Item> requested) {
        List<ReservationWithItems.StoredItem> storedItems = stored.getItems();
        if (stored.getStatus() == ReservationStatus.RELEASED && storedItems.isEmpty()) {
            return ExistingJudgement.RELEASE_MARKER;
        }
        List<ReserveRequest.Item> sortedRequested = sortByProductId(requested);
        List<ReservationWithItems.StoredItem> sortedStored = new ArrayList<>(storedItems);
        sortedStored.sort(Comparator.comparing(ReservationWithItems.StoredItem::productId));
        if (sortedStored.size() != sortedRequested.size()) {
            return ExistingJudgement.CONFLICT;
        }
        for (int i = 0; i < sortedStored.size(); i++) {
            ReservationWithItems.StoredItem storedItem = sortedStored.get(i);
            ReserveRequest.Item requestedItem = sortedRequested.get(i);
            if (!storedItem.productId().equals(requestedItem.productId())
                    || storedItem.quantity() != requestedItem.quantity()) {
                return ExistingJudgement.CONFLICT;
            }
        }
        return ExistingJudgement.STORED_RESULT;
    }

    /**
     * 새 예약을 판단한다. 재고 서비스가 항목의 재고 행을 잠그고 읽은 수량으로 판단한다.
     * 재고 데이터에 없는 상품은 stockByProductId에 키가 없다.
     *
     * <p>없는 상품이나 부족한 상품이 하나라도 있으면 거절이다. 사유는 없는 상품이 하나라도 있으면 상품 없음, 아니면 재고 부족이다.
     * 두 목록은 모두 채우고, 상품 ID 순이다. 모든 항목의 재고가 충분하면 예약됨이다.
     */
    public static ReservationDecision decide(List<ReserveRequest.Item> items, Map<String, Integer> stockByProductId) {
        List<String> shortage = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Map<String, ReservationReason> results = new HashMap<>();
        for (ReserveRequest.Item item : sortByProductId(items)) {
            Integer quantity = stockByProductId.get(item.productId());
            if (quantity == null) {
                missing.add(item.productId());
                results.put(item.productId(), ReservationReason.PRODUCT_NOT_FOUND);
            } else if (quantity < item.quantity()) {
                shortage.add(item.productId());
                results.put(item.productId(), ReservationReason.OUT_OF_STOCK);
            }
        }
        if (shortage.isEmpty() && missing.isEmpty()) {
            return new ReservationDecision(ReservationStatus.RESERVED, null, List.of(), List.of(), Map.of());
        }
        ReservationReason reason =
                missing.isEmpty() ? ReservationReason.OUT_OF_STOCK : ReservationReason.PRODUCT_NOT_FOUND;
        return new ReservationDecision(
                ReservationStatus.REJECTED, reason, List.copyOf(shortage), List.copyOf(missing), Map.copyOf(results));
    }

    /** 항목을 상품 ID 순으로 정렬한 새 목록을 돌려준다. 재고 행은 언제나 이 순서로 잠근다. 순서가 다르면 교착이 생긴다. */
    public static List<ReserveRequest.Item> sortByProductId(List<ReserveRequest.Item> items) {
        List<ReserveRequest.Item> sorted = new ArrayList<>(items);
        sorted.sort(BY_PRODUCT_ID);
        return sorted;
    }

    /** 예약됨(RESERVED) 또는 거절됨(REJECTED)이다. */
    public ReservationStatus status() {
        return status;
    }

    /** 거절 사유다. 예약됨이면 null이다. */
    public ReservationReason reason() {
        return reason;
    }

    /** 재고가 부족한 상품 ID. 상품 ID 순이다. */
    public List<String> shortageProductIds() {
        return shortageProductIds;
    }

    /** 재고 데이터에 없는 상품 ID. 상품 ID 순이다. */
    public List<String> missingProductIds() {
        return missingProductIds;
    }

    /** 항목 하나의 결과다. 부족했으면 OUT_OF_STOCK, 없었으면 PRODUCT_NOT_FOUND, 그 밖에는 null이다. */
    public ReservationReason resultOf(String productId) {
        return results.get(productId);
    }
}
