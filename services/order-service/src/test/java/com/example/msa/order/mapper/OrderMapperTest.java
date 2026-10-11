package com.example.msa.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.msa.order.OracleTestContainer;
import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.dto.OrderWithItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

/**
 * 주문의 SQL이 실제 Oracle에서 맞게 도는지 본다. 테스트 하나가 끝나면 변경은 롤백된다.
 *
 * <p>값은 테스트 케이스 문서의 값을 쓴다. 고객 C1, 상품 A 3개와 상품 B 2개는 TC-001의 값이다.
 * 주문 번호를 시퀀스로 만드는 것과 주문·항목을 {@code <collection>}으로 한 번에 읽는 것은 코딩 규약 3-2절이다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(OracleTestContainer.class)
@ActiveProfiles("test")
class OrderMapperTest {

    private static final String C1 = "customer-c1";
    private static final String C2 = "customer-c2";
    private static final String KEY_1 = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("시퀀스로 주문 번호를 받아 주문과 두 항목(A 3개, B 2개)을 넣고, 한 번에 읽는다")
    void insertsOrderWithItemsAndReadsAtOnce() {
        OrderWithItems order = pendingOrder(C1, KEY_1);

        assertThat(orderMapper.insertOrder(order)).isEqualTo(1);
        long orderNo = order.getOrderNo();
        assertThat(orderNo).isPositive();
        // 일부러 B를 먼저 넣어서, 읽을 때 상품 ID 순으로 나오는지 본다
        orderMapper.insertOrderItem(orderNo, "B", 2);
        orderMapper.insertOrderItem(orderNo, "A", 3);

        OrderWithItems read = orderMapper.selectOrderWithItems(orderNo);

        assertThat(read.getOrderNo()).isEqualTo(orderNo);
        assertThat(read.getCustomerId()).isEqualTo(C1);
        assertThat(read.getRequestKey()).isEqualTo(KEY_1);
        assertThat(read.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(read.getReason()).isNull();
        assertThat(read.getReleaseStatus()).isNull();
        assertThat(read.getItems())
                .extracting(
                        OrderWithItems.StoredItem::productId,
                        OrderWithItems.StoredItem::quantity,
                        OrderWithItems.StoredItem::rejectReason)
                .containsExactly(tuple("A", 3, null), tuple("B", 2, null));
    }

    @Test
    @DisplayName("주문 번호는 시퀀스에서 받아 주문마다 다르다")
    void orderNumbersDiffer() {
        OrderWithItems first = pendingOrder(C1, KEY_1);
        OrderWithItems second = pendingOrder(C1, "22222222-2222-2222-2222-222222222222");

        orderMapper.insertOrder(first);
        orderMapper.insertOrder(second);

        assertThat(second.getOrderNo()).isGreaterThan(first.getOrderNo());
    }

    @Test
    @DisplayName("주문의 상태를 확정(CONFIRMED)으로 바꾼다")
    void updatesStatusToConfirmed() {
        OrderWithItems order = pendingOrder(C1, KEY_1);
        orderMapper.insertOrder(order);
        orderMapper.insertOrderItem(order.getOrderNo(), "A", 3);

        assertThat(orderMapper.updateOrderResult(order.getOrderNo(), OrderStatus.CONFIRMED, null, null))
                .isEqualTo(1);

        OrderWithItems read = orderMapper.selectOrderWithItems(order.getOrderNo());
        assertThat(read.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(read.getReason()).isNull();
    }

    @Test
    @DisplayName("거절된 주문의 항목별 거절 사유를 저장한다: Z는 PRODUCT_NOT_FOUND, B는 OUT_OF_STOCK, A는 빈 값")
    void savesRejectReasonPerItem() {
        // TC-015 변형의 값이다. 상품 A 3개, 상품 Z 1개, 상품 B 2개를 주문했고, Z는 없고 B는 부족하다.
        OrderWithItems order = pendingOrder(C1, KEY_1);
        orderMapper.insertOrder(order);
        long orderNo = order.getOrderNo();
        orderMapper.insertOrderItem(orderNo, "A", 3);
        orderMapper.insertOrderItem(orderNo, "Z", 1);
        orderMapper.insertOrderItem(orderNo, "B", 2);

        assertThat(orderMapper.updateItemRejectReason(orderNo, "Z", OrderReason.PRODUCT_NOT_FOUND))
                .isEqualTo(1);
        assertThat(orderMapper.updateItemRejectReason(orderNo, "B", OrderReason.OUT_OF_STOCK))
                .isEqualTo(1);
        orderMapper.updateOrderResult(orderNo, OrderStatus.REJECTED, OrderReason.PRODUCT_NOT_FOUND, null);

        OrderWithItems read = orderMapper.selectOrderWithItems(orderNo);
        assertThat(read.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(read.getReason()).isEqualTo(OrderReason.PRODUCT_NOT_FOUND);
        assertThat(read.getItems())
                .extracting(OrderWithItems.StoredItem::productId, OrderWithItems.StoredItem::rejectReason)
                .containsExactly(
                        tuple("A", null),
                        tuple("B", OrderReason.OUT_OF_STOCK),
                        tuple("Z", OrderReason.PRODUCT_NOT_FOUND));
    }

    @Test
    @DisplayName("같은 고객이 같은 요청 키로 주문을 두 번 넣으면 고유 제약 위반 예외가 나고, 다른 고객의 같은 키는 들어간다")
    void sameCustomerAndKeyViolatesUniqueKey() {
        assertThat(orderMapper.insertOrder(pendingOrder(C1, KEY_1))).isEqualTo(1);

        assertThatThrownBy(() -> orderMapper.insertOrder(pendingOrder(C1, KEY_1)))
                .isInstanceOf(DuplicateKeyException.class);

        // 키는 고객마다 따로 본다
        assertThat(orderMapper.insertOrder(pendingOrder(C2, KEY_1))).isEqualTo(1);
    }

    @Test
    @DisplayName("고객과 요청 키로 주문과 항목을 한 번에 읽는다")
    void readsOrderWithItemsByCustomerAndKey() {
        OrderWithItems order = pendingOrder(C1, KEY_1);
        orderMapper.insertOrder(order);
        orderMapper.insertOrderItem(order.getOrderNo(), "B", 2);
        orderMapper.insertOrderItem(order.getOrderNo(), "A", 3);

        OrderWithItems read = orderMapper.selectOrderWithItemsByCustomerKey(C1, KEY_1);

        assertThat(read.getOrderNo()).isEqualTo(order.getOrderNo());
        assertThat(read.getItems())
                .extracting(OrderWithItems.StoredItem::productId, OrderWithItems.StoredItem::quantity)
                .containsExactly(tuple("A", 3), tuple("B", 2));
        // 다른 고객의 같은 키로는 이 주문이 읽히지 않는다
        assertThat(orderMapper.selectOrderWithItemsByCustomerKey(C2, KEY_1)).isNull();
    }

    @Test
    @DisplayName("없는 주문 번호를 읽으면 결과가 없다")
    void unknownOrderNumberReturnsNothing() {
        assertThat(orderMapper.selectOrderWithItems(999_999L)).isNull();
    }

    private static OrderWithItems pendingOrder(String customerId, String requestKey) {
        OrderWithItems order = new OrderWithItems();
        order.setCustomerId(customerId);
        order.setRequestKey(requestKey);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }
}
