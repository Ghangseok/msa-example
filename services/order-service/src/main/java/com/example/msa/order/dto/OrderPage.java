package com.example.msa.order.dto;

import com.example.msa.order.domain.PageRequest;
import java.util.List;

/**
 * 목록 응답 봉투다. specs/002-view-orders/contracts/order-query-api.yaml의 OrderPage와 같다. 관리자 목록도 같은 모양이라서 형식 매개변수로 만들었다.
 * 주문이 하나도 없으면 {@code orders}는 빈 배열이고 {@code hasNext}는 false다.
 *
 * @param hasNext 다음 쪽이 있으면 true
 */
public record OrderPage<T>(List<T> orders, int page, int size, boolean hasNext) {

    /** {@code size + 1}개까지 읽은 목록에서 {@code hasNext}를 정하고, {@code size}개로 자른다. */
    public static <T> OrderPage<T> of(List<T> read, PageRequest request) {
        boolean hasNext = read.size() > request.size();
        List<T> orders = hasNext ? read.subList(0, request.size()) : read;
        return new OrderPage<>(List.copyOf(orders), request.page(), request.size(), hasNext);
    }
}
