package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 4.1: 로그인한 사람만 주문한다. 이 스토리에서 검증하는 줄은 첫 When/Then이다.
 * 다른 고객의 주문 조회(404), 자기 주문 목록 조회, 고객의 관리자 조회(403), 관리자의 실패 주문 조회는 티켓 002에서 검증한다.
 */
class UnauthenticatedOrderIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-010 토큰 없이 주문하면 401을 받고 주문은 기록되지 않는다")
    void orderWithoutTokenIsRejected() {
        // Given 재고 서비스 자리는 RESERVED를 돌려줄 준비가 되어 있다. 그래도 요청이 가면 안 된다.
        stubReserve(reserved());

        // When 토큰 없이 주문한다
        Reply reply = postOrder(null, newKey(), orderJson(new Item("A", 3)));

        // Then 401을 받는다. 오류 응답 본문은 Problem Details 형식이고, 토큰이 없으면 code는 UNAUTHORIZED다 (공통 전제)
        assertThat(reply.status()).isEqualTo(401);
        assertThat(reply.contentType()).startsWith("application/problem+json");
        assertThat(reply.text("$.code")).isEqualTo("UNAUTHORIZED");

        // And 주문은 기록되지 않는다
        assertThat(orderCount()).isZero();
        // And 재고 서비스에는 요청이 가지 않았다
        assertThat(reserveRequests()).isEmpty();
    }
}
