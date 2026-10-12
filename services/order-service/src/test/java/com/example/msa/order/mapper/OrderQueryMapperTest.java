package com.example.msa.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.msa.order.OracleTestContainer;
import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.dto.OrderWithItems;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

/**
 * 조회 SQL이 실제 Oracle에서 맞게 도는지 본다. 테스트 하나가 끝나면 변경은 롤백된다. 인수 시나리오를 직접 검증하는 테스트가 아니라서
 * 테스트 케이스 번호를 붙이지 않는다. 값은 테스트 케이스 문서의 값(고객 C1·C2, 상품 A 3개, 상품 Z 1개, 상품 B 2개)을 쓴다.
 * 항목의 순서는 기준 문서에 없어서 단언하지 않는다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(OracleTestContainer.class)
@ActiveProfiles("test")
class OrderQueryMapperTest {

    private static final String C1 = "customer-c1";
    private static final String C2 = "customer-c2";

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("거절 주문의 상태, 사유, 항목별 거절 사유(OUT_OF_STOCK, PRODUCT_NOT_FOUND)를 읽는다")
    void readsRejectedOrderWithItemRejectReasons() {
        long orderNo = insertOrder(C1);
        orderMapper.insertOrderItem(orderNo, "A", 3);
        orderMapper.insertOrderItem(orderNo, "Z", 1);
        orderMapper.insertOrderItem(orderNo, "B", 2);
        orderMapper.updateItemRejectReason(orderNo, "Z", OrderReason.PRODUCT_NOT_FOUND);
        orderMapper.updateItemRejectReason(orderNo, "B", OrderReason.OUT_OF_STOCK);
        orderMapper.updateOrderResult(orderNo, OrderStatus.REJECTED, OrderReason.PRODUCT_NOT_FOUND, null);

        OrderWithItems read = orderMapper.selectCustomerOrder(orderNo, C1);

        assertThat(read.getOrderNo()).isEqualTo(orderNo);
        assertThat(read.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(read.getReason()).isEqualTo(OrderReason.PRODUCT_NOT_FOUND);
        assertThat(read.getItems())
                .extracting(
                        OrderWithItems.StoredItem::productId,
                        OrderWithItems.StoredItem::quantity,
                        OrderWithItems.StoredItem::rejectReason)
                .containsExactlyInAnyOrder(
                        tuple("A", 3, null),
                        tuple("Z", 1, OrderReason.PRODUCT_NOT_FOUND),
                        tuple("B", 2, OrderReason.OUT_OF_STOCK));
    }

    @Test
    @DisplayName("주문 번호와 고객이 모두 맞아야 읽힌다. 고객이 다르면 결과가 없다")
    void readsOnlyWhenOrderNumberAndCustomerBothMatch() {
        long orderNo = insertOrder(C1);
        orderMapper.insertOrderItem(orderNo, "A", 3);

        assertThat(orderMapper.selectCustomerOrder(orderNo, C1)).isNotNull();
        assertThat(orderMapper.selectCustomerOrder(orderNo, C2)).isNull();
    }

    @Test
    @DisplayName("CREATED_AT을 읽으면 주문 시각이 비어 있지 않다")
    void readsCreatedAt() {
        long orderNo = insertOrder(C1);
        orderMapper.insertOrderItem(orderNo, "A", 3);

        assertThat(orderMapper.selectCustomerOrder(orderNo, C1).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("목록은 고객 조건으로 읽는다. 두 고객의 주문이 있어도 자기 주문만 읽힌다")
    void listReadsOnlyCustomersOrders() {
        long c1Order = insertOrder(C1);
        orderMapper.insertOrderItem(c1Order, "A", 3);
        long c2Order = insertOrder(C2);
        orderMapper.insertOrderItem(c2Order, "A", 1);

        assertThat(orderMapper.selectCustomerOrders(C1, 0, 20))
                .extracting(OrderWithItems::getOrderNo)
                .containsExactly(c1Order);
        assertThat(orderMapper.selectCustomerOrders(C2, 0, 20))
                .extracting(OrderWithItems::getOrderNo)
                .containsExactly(c2Order);
    }

    @Test
    @DisplayName("주문이 없는 고객의 목록은 빈 결과다")
    void listIsEmptyForCustomerWithoutOrders() {
        long c1Order = insertOrder(C1);
        orderMapper.insertOrderItem(c1Order, "A", 3);

        assertThat(orderMapper.selectCustomerOrders(C2, 0, 20)).isEmpty();
    }

    @Test
    @DisplayName("목록은 처리중 주문과 실패 주문도 읽는다")
    void listReadsPendingAndFailedOrders() {
        long pending = insertOrder(C1);
        orderMapper.insertOrderItem(pending, "A", 3);
        long failed = insertOrder(C1);
        orderMapper.insertOrderItem(failed, "A", 3);
        orderMapper.updateOrderResult(failed, OrderStatus.FAILED, OrderReason.RETRY_LATER, null);

        assertThat(orderMapper.selectCustomerOrders(C1, 0, 20))
                .extracting(OrderWithItems::getOrderNo, OrderWithItems::getStatus)
                .containsExactlyInAnyOrder(tuple(pending, OrderStatus.PENDING), tuple(failed, OrderStatus.FAILED));
    }

    @Test
    @DisplayName("목록은 주문 번호가 큰 순서이고, 항목이 여러 개인 주문이 쪽 경계에서 잘리지 않는다")
    void listIsNewestFirstAndKeepsOrdersWhole() {
        insertOrderWithTwoItems();
        long second = insertOrderWithTwoItems();
        long third = insertOrderWithTwoItems();

        // limit 2: 항목 2개짜리 주문 3개를 2로 읽으면 주문 2개와 각 항목이 온다
        List<OrderWithItems> page = orderMapper.selectCustomerOrders(C1, 0, 2);

        assertThat(page).extracting(OrderWithItems::getOrderNo).containsExactly(third, second);
        assertThat(page).allSatisfy(order -> assertThat(order.getItems()).hasSize(2));
    }

    @Test
    @DisplayName("size + 1개를 읽으면 다음 쪽이 있는지 알 수 있고, offset만큼 건너뛴다")
    void listSkipsOffsetAndReadsOneMore() {
        long first = insertOrderWithTwoItems();
        insertOrderWithTwoItems();
        insertOrderWithTwoItems();

        // size 2이면 limit 3(size + 1)으로 읽는다. 3개가 오면 다음 쪽이 있다
        assertThat(orderMapper.selectCustomerOrders(C1, 0, 3)).hasSize(3);
        // 둘째 쪽(offset 2): 가장 오래된 주문 하나만 남는다
        assertThat(orderMapper.selectCustomerOrders(C1, 2, 3))
                .extracting(OrderWithItems::getOrderNo)
                .containsExactly(first);
    }

    private long insertOrderWithTwoItems() {
        long orderNo = insertOrder(C1);
        orderMapper.insertOrderItem(orderNo, "A", 3);
        orderMapper.insertOrderItem(orderNo, "B", 2);
        return orderNo;
    }

    private long insertOrder(String customerId) {
        OrderWithItems order = new OrderWithItems();
        order.setCustomerId(customerId);
        order.setRequestKey(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insertOrder(order);
        return order.getOrderNo();
    }
}
