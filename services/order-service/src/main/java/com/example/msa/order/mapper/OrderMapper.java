package com.example.msa.order.mapper;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import com.example.msa.order.dto.OrderWithItems;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 주문과 주문 항목을 다루는 Mapper다. SQL은 resources/mapper/OrderMapper.xml에 있다. */
@Mapper
public interface OrderMapper {

    /**
     * 주문을 넣는다. 주문 번호는 시퀀스 ORDERS_SEQ에서 받아 {@code order.orderNo}에 채워 준다.
     * 같은 고객이 같은 요청 키로 이미 넣었으면 고유 제약 위반이다.
     */
    int insertOrder(OrderWithItems order);

    /** 주문 항목을 넣는다. 거절 사유는 비워 두고, 예약 결과를 받은 뒤에 채운다. */
    int insertOrderItem(
            @Param("orderNo") long orderNo, @Param("productId") String productId, @Param("quantity") int quantity);

    /** 주문과 항목을 상품 ID 순으로 한 번에 읽는다. 주문이 없으면 null이다. */
    OrderWithItems selectOrderWithItems(@Param("orderNo") long orderNo);

    /** 고객과 주문 요청 키로 주문과 항목을 상품 ID 순으로 한 번에 읽는다. 없으면 null이다. 키는 고객마다 따로 본다. */
    OrderWithItems selectOrderWithItemsByCustomerKey(
            @Param("customerId") String customerId, @Param("requestKey") String requestKey);

    /** 거절된 주문의 항목 하나에 거절 사유를 저장한다. 부족했으면 OUT_OF_STOCK, 없었으면 PRODUCT_NOT_FOUND다. */
    int updateItemRejectReason(
            @Param("orderNo") long orderNo,
            @Param("productId") String productId,
            @Param("rejectReason") OrderReason rejectReason);

    /** 주문의 결과를 저장한다(상태, 사유, 해제 결과, 바꾼 시각). 사유와 해제 결과가 없으면 null을 넘긴다. */
    int updateOrderResult(
            @Param("orderNo") long orderNo,
            @Param("status") OrderStatus status,
            @Param("reason") OrderReason reason,
            @Param("releaseStatus") ReleaseStatus releaseStatus);
}
