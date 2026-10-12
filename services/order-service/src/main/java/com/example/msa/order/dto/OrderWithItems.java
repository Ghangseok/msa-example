package com.example.msa.order.dto;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 하나와 그 항목들이다. MyBatis가 {@code <collection>}으로 한 번에 읽는 1:N 부모라서 record가 아니라 일반 클래스다.
 * 주문을 넣을 때는 주문 번호를 시퀀스에서 받아 {@code orderNo}에 채우는 매개변수로도 쓴다. 항목은 상품 ID 순으로 들어온다.
 */
public class OrderWithItems {

    /**
     * 주문 항목 하나다.
     *
     * @param rejectReason 거절된 주문에서 이 상품이 부족했으면 OUT_OF_STOCK, 없었으면 PRODUCT_NOT_FOUND다. 그 밖에는 null이다
     */
    public record StoredItem(String productId, int quantity, OrderReason rejectReason) {}

    private long orderNo;
    private String customerId;
    private String requestKey;
    private OrderStatus status;
    private OrderReason reason;
    private ReleaseStatus releaseStatus;
    private LocalDateTime createdAt;
    private List<StoredItem> items = new ArrayList<>();

    public long getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(long orderNo) {
        this.orderNo = orderNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderReason getReason() {
        return reason;
    }

    public void setReason(OrderReason reason) {
        this.reason = reason;
    }

    public ReleaseStatus getReleaseStatus() {
        return releaseStatus;
    }

    public void setReleaseStatus(ReleaseStatus releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<StoredItem> getItems() {
        return items;
    }

    public void setItems(List<StoredItem> items) {
        this.items = items;
    }
}
