package com.example.msa.inventory.dto;

import com.example.msa.inventory.domain.ReservationReason;
import com.example.msa.inventory.domain.ReservationStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * 예약 기록 하나와 그 항목들이다. MyBatis가 {@code <collection>}으로 한 번에 읽는 1:N 부모라서 record가 아니라 일반 클래스다.
 * 항목은 상품 ID 순으로 들어온다. 해제 표식(항목 없는 해제됨 기록)은 항목이 빈 목록이다.
 */
public class ReservationWithItems {

    /**
     * 예약 항목 하나다.
     *
     * @param result 거절된 예약에서 부족했으면 OUT_OF_STOCK, 없었으면 PRODUCT_NOT_FOUND다. 그 밖에는 null이다
     */
    public record StoredItem(String productId, int quantity, ReservationReason result) {}

    private long orderNo;
    private ReservationStatus status;
    private ReservationReason reason;
    private List<StoredItem> items = new ArrayList<>();

    public long getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(long orderNo) {
        this.orderNo = orderNo;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public ReservationReason getReason() {
        return reason;
    }

    public void setReason(ReservationReason reason) {
        this.reason = reason;
    }

    public List<StoredItem> getItems() {
        return items;
    }

    public void setItems(List<StoredItem> items) {
        this.items = items;
    }
}
