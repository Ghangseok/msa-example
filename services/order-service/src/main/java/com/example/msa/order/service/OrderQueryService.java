package com.example.msa.order.service;

import com.example.msa.order.domain.OrderNumbers;
import com.example.msa.order.domain.PageRequest;
import com.example.msa.order.dto.OrderDetailResponse;
import com.example.msa.order.dto.OrderPage;
import com.example.msa.order.dto.OrderWithItems;
import com.example.msa.order.mapper.OrderMapper;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 조회의 흐름을 조립한다. 읽기 전용 트랜잭션 하나로 끝나고, 재고 서비스를 부르지 않는다.
 * 읽기 전용 트랜잭션은 클래스가 아니라 메서드마다 붙인다.
 */
@Service
public class OrderQueryService {

    private final OrderMapper orderMapper;

    public OrderQueryService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 고객 본인의 주문 하나를 찾는다. 주문 번호 글자가 숫자가 아니면 DB를 부르지 않고 빈 값을 돌려준다.
     * 고객 조건이 SQL에 있어서 다른 고객의 주문도 없는 주문과 같은 빈 값이 된다.
     */
    @Transactional(readOnly = true)
    public Optional<OrderDetailResponse> findMine(String customerId, String orderNoText) {
        OptionalLong orderNo = OrderNumbers.parse(orderNoText);
        if (orderNo.isEmpty()) {
            return Optional.empty();
        }
        OrderWithItems order = orderMapper.selectCustomerOrder(orderNo.getAsLong(), customerId);
        return Optional.ofNullable(order).map(OrderDetailResponse::from);
    }

    /** 고객 본인의 주문 목록 한 쪽을 주문 번호가 큰 순서로 읽는다. {@code size + 1}개를 읽어 다음 쪽이 있는지 정한다. */
    @Transactional(readOnly = true)
    public OrderPage<OrderDetailResponse> listMine(String customerId, PageRequest request) {
        List<OrderDetailResponse> read =
                orderMapper.selectCustomerOrders(customerId, request.offset(), request.fetchSize()).stream()
                        .map(OrderDetailResponse::from)
                        .toList();
        return OrderPage.of(read, request);
    }
}
