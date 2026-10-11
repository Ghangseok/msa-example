package com.example.msa.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.dto.OrderItemRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 주문 항목 검사의 단위 테스트다. Spring도 DB도 쓰지 않는다.
 * 값은 TC-014의 다섯 경우와 TC-001의 올바른 항목(상품 A 3개, 상품 B 2개)이다.
 * 규칙은 유스케이스 6절 업무 규칙이다. 주문 항목은 1개 이상 20개 이하이고, 각 수량은 1 이상 99 이하이며,
 * 같은 상품은 한 줄에만 나온다.
 */
class OrderItemsTest {

    @Test
    @DisplayName("올바른 항목(A 3개, B 2개)은 검사를 통과한다")
    void validItemsPass() {
        List<OrderItemRequest> items = List.of(new OrderItemRequest("A", 3), new OrderItemRequest("B", 2));

        assertThat(OrderItems.validate(items)).isEmpty();
    }

    @Test
    @DisplayName("항목이 하나도 없으면 검사에 실패한다")
    void emptyItemsFail() {
        assertThat(OrderItems.validate(List.of())).isNotEmpty();
    }

    @Test
    @DisplayName("수량이 0인 항목이 있으면 검사에 실패한다")
    void zeroQuantityFails() {
        assertThat(OrderItems.validate(List.of(new OrderItemRequest("A", 0)))).isNotEmpty();
    }

    @Test
    @DisplayName("같은 상품이 두 줄에 나오면 검사에 실패한다")
    void duplicateProductFails() {
        List<OrderItemRequest> items = List.of(new OrderItemRequest("A", 3), new OrderItemRequest("A", 2));

        assertThat(OrderItems.validate(items)).isNotEmpty();
    }

    @Test
    @DisplayName("항목이 21개이면 검사에 실패한다")
    void twentyOneItemsFail() {
        assertThat(OrderItems.validate(items(21))).isNotEmpty();
    }

    @Test
    @DisplayName("항목이 20개이면 검사를 통과한다")
    void twentyItemsPass() {
        assertThat(OrderItems.validate(items(20))).isEmpty();
    }

    @Test
    @DisplayName("수량이 100인 항목이 있으면 검사에 실패한다")
    void quantityOfHundredFails() {
        assertThat(OrderItems.validate(List.of(new OrderItemRequest("A", 100)))).isNotEmpty();
    }

    @Test
    @DisplayName("수량이 99인 항목은 검사를 통과한다")
    void quantityOfNinetyNinePasses() {
        assertThat(OrderItems.validate(List.of(new OrderItemRequest("A", 99)))).isEmpty();
    }

    @Test
    @DisplayName("상품 ID가 빈 항목이 있으면 검사에 실패한다 (TC-022의 값)")
    void emptyProductIdFails() {
        assertThat(OrderItems.validate(List.of(new OrderItemRequest("", 1)))).isNotEmpty();
    }

    private static List<OrderItemRequest> items(int count) {
        List<OrderItemRequest> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(new OrderItemRequest("P" + i, 1));
        }
        return items;
    }
}
