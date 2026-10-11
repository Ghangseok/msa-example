package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 시나리오 2.2: 주문 서비스는 재고 서비스 호출에 추적 정보(W3C traceparent 헤더)를 넘긴다. */
class TracePropagationIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-024 주문 서비스는 재고 서비스 호출에 추적 정보를 넘긴다")
    void reserveRequestCarriesTraceparent() {
        // Given 재고 서비스가 예약 요청에 RESERVED를 돌려준다
        stubReserve(reserved());

        // When 고객이 상품 A 1개를 주문한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 1));
        assertThat(reply.status()).isEqualTo(201);

        // Then 재고 서비스가 받은 예약 요청에 W3C traceparent 헤더가 있다
        List<LoggedRequest> requests = reserveRequests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).containsHeader("traceparent")).isTrue();
    }
}
