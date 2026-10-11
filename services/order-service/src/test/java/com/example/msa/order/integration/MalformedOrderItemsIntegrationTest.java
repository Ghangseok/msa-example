package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 시나리오 5.2: 형식이 틀린 주문 항목은 기록되지 않는다. 상품 ID가 빈 항목과 수량이 정수가 아닌(1.5) 항목을 각각 요청한다.
 * 수량 1.5는 {@code orderJson}으로 만들 수 없어서(수량이 int다) 본문 글자를 직접 쓴다.
 */
class MalformedOrderItemsIntegrationTest extends InventoryWireMock {

    static Stream<Arguments> malformedOrders() {
        return Stream.of(
                Arguments.of("상품 ID가 빈 항목이 있는 주문", "{\"items\":[{\"productId\":\"\",\"quantity\":1}]}"),
                Arguments.of("수량이 정수가 아닌(1.5) 항목이 있는 주문", "{\"items\":[{\"productId\":\"A\",\"quantity\":1.5}]}"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedOrders")
    @DisplayName("TC-022 형식이 틀린 주문 항목은 기록되지 않는다")
    void malformedItemsAreNotRecorded(String caseName, String body) {
        // 재고 서비스 자리는 RESERVED를 돌려줄 준비가 되어 있다. 그래도 요청이 가면 안 된다.
        stubReserve(reserved());

        // When 형식이 틀린 주문을 요청한다
        Reply reply = postOrder(customer(C1), newKey(), body);

        // Then 400을 받는다
        assertThat(reply.status()).isEqualTo(400);
        // And 주문은 기록되지 않고 재고 서비스에 요청이 가지 않는다
        assertThat(orderCount()).isZero();
        assertThat(reserveRequests()).isEmpty();
    }
}
