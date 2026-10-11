package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 시나리오 5.1: 잘못된 주문 항목은 기록되지 않는다. 다섯 경우를 매개변수 테스트 하나로 돌린다 [제안].
 * 같은 검사를 다섯 번 되풀이하지 않으려는 것이다.
 */
class InvalidOrderItemsIntegrationTest extends InventoryWireMock {

    static Stream<Arguments> invalidOrders() {
        List<Item> twentyOne = new ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            twentyOne.add(new Item("P" + i, 1));
        }
        return Stream.of(
                Arguments.of("항목이 하나도 없는 주문", orderJson()),
                Arguments.of("수량이 0인 항목이 있는 주문", orderJson(new Item("A", 0))),
                Arguments.of("같은 상품이 두 줄에 나오는 주문", orderJson(new Item("A", 3), new Item("A", 2))),
                Arguments.of("항목이 21개인 주문", orderJson(twentyOne.toArray(new Item[0]))),
                Arguments.of("수량이 100인 항목이 있는 주문", orderJson(new Item("A", 100))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidOrders")
    @DisplayName("TC-014 잘못된 주문 항목은 기록되지 않는다")
    void invalidItemsAreNotRecorded(String caseName, String body) {
        // 재고 서비스 자리는 RESERVED를 돌려줄 준비가 되어 있다. 그래도 요청이 가면 안 된다.
        stubReserve(reserved());

        // When 잘못된 주문을 요청한다
        Reply reply = postOrder(customer(C1), newKey(), body);

        // Then 400을 받는다
        assertThat(reply.status()).isEqualTo(400);
        // And 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다
        assertThat(orderCount()).isZero();
        assertThat(reserveRequests()).isEmpty();
    }
}
