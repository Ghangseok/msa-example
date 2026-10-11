package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 6.2: 거부된 요청의 키는 남지 않고, 같은 키의 내용 비교는 항목 순서를 보지 않는다.
 * 두 경우를 메서드 두 개로 나눈다 [제안]. 어느 경우가 실패했는지 바로 보이게 하려는 것이다.
 */
class IdempotencyKeyReuseIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-023 거부된 요청의 키는 남지 않는다: 400을 받은 키로 올바른 주문을 보내면 새 주문이 된다")
    void rejectedRequestDoesNotKeepItsKey() {
        stubReserve(reserved());
        String k4 = newKey();

        // When 고객이 요청 키 K4로 수량이 0인 항목이 있는 주문을 보내 400을 받고
        Reply rejected = placeOrder(C1, k4, new Item("A", 0));
        assertThat(rejected.status()).isEqualTo(400);
        assertThat(orderCount()).isZero();

        // 같은 키 K4로 상품 A 1개를 주문한다
        Reply corrected = placeOrder(C1, k4, new Item("A", 1));

        // Then 주문 번호를 받고 그 주문이 기록된다
        assertThat(corrected.status()).isEqualTo(201);
        assertThat(orderRow(corrected.orderNo())).isNotNull();
        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-023 같은 키의 내용 비교는 항목 순서를 보지 않는다: A, B 순서와 B, A 순서는 같은 요청이다")
    void itemOrderIsNotComparedForTheSameKey() {
        stubReserve(reserved());
        String k5 = newKey();

        // When 고객이 요청 키 K5로 상품 A 1개와 상품 B 1개를 "A, B" 순서로 주문하고
        Reply first = placeOrder(C1, k5, new Item("A", 1), new Item("B", 1));
        // 같은 키 K5로 "B, A" 순서로 같은 요청을 한 번 더 보낸다
        Reply again = placeOrder(C1, k5, new Item("B", 1), new Item("A", 1));

        // Then 두 응답의 주문 번호가 같다 (422가 아니다)
        assertThat(first.status()).isEqualTo(201);
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.orderNo()).isEqualTo(first.orderNo());
        // And 주문은 하나만 기록된다
        assertThat(orderCount()).isEqualTo(1);
    }
}
