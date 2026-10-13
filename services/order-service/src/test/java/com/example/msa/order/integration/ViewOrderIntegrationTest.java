package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 스토리 1: 고객이 자기 주문 하나를 조회한다. 시나리오 1.1~1.5다.
 *
 * <p>응답 필드 이름과 값({@code status}의 {@code REJECTED} 같은 영어 이름)은 계약 파일
 * specs/002-view-orders/contracts/order-query-api.yaml이 정했다. 테스트 케이스 문서는 한국어 개념만 적었다.
 * "주문이 있다"는 상태는 {@code OrderMapper}로 직접 저장해서 만든다. 문서의 "주문 1004" 같은 번호는 저장 결과의 번호로 대신한다.
 */
class ViewOrderIntegrationTest extends ViewTestSupport {

    private static final Logger log = LoggerFactory.getLogger(ViewOrderIntegrationTest.class);

    private static final String NOT_FOUND_CODE = "ORDER_NOT_FOUND";

    @Test
    @DisplayName("TC-002 항목 하나라도 재고가 부족하면 주문 전체가 거절되고 기록된다: 그 거절 주문을 C1이 조회할 수 있다")
    void customerCanViewRejectedOrder() {
        // Given 상품 A 3개와 상품 B 2개를 주문해 "거절"로 기록됐다 (B는 재고 부족)
        long orderNo = seed(
                C1,
                OrderStatus.REJECTED,
                OrderReason.OUT_OF_STOCK,
                null,
                item("A", 3),
                item("B", 2, OrderReason.OUT_OF_STOCK));

        // When C1이 그 주문을 조회한다
        Reply reply = get(customer(C1), "/orders/" + orderNo);

        // Then 그 주문이 "거절" 상태로 나온다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("OUT_OF_STOCK");
        assertThat(reply.texts("$.shortageProductIds")).containsExactly("B");
        assertThat(itemsOf(reply)).containsExactlyInAnyOrder(new Item("A", 3), new Item("B", 2));
    }

    @Test
    @DisplayName("TC-010 로그인한 사람만 주문하고, 자기 주문만 본다: C2가 C1의 주문을 조회하면 \"없음\"(404)이다")
    void otherCustomersOrderIsNotFound() {
        // Given C1의 주문이 있다
        long orderNo = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When C2가 그 주문을 조회한다
        Reply reply = get(customer(C2), "/orders/" + orderNo);

        // Then "없음"(404)을 받는다 (오류 응답 본문은 Problem Details 형식이다. 공통 전제)
        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.contentType()).startsWith("application/problem+json");
        assertThat(reply.text("$.code")).isEqualTo(NOT_FOUND_CODE);
    }

    @Test
    @DisplayName("TC-026 없는 주문 번호와 숫자가 아닌 주문 번호는 \"없음\"이다")
    void unknownAndNonNumericOrderNumbersAreNotFound() {
        // Given 주문 9999는 없다. 다른 고객(C2)의 주문은 있다
        assertThat(orderRow(9999)).isNull();
        long othersOrderNo = seed(C2, OrderStatus.CONFIRMED, null, null, item("A", 3));
        Reply others = get(customer(C1), "/orders/" + othersOrderNo);

        // When C1이 주문 9999를 조회한다
        Reply unknown = get(customer(C1), "/orders/9999");

        // Then "없음"(404)을 받는다. 다른 고객의 주문을 조회했을 때와 같은 응답이다
        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.status()).isEqualTo(others.status());
        assertThat(unknown.text("$.type")).isEqualTo(others.text("$.type"));
        assertThat(unknown.text("$.title")).isEqualTo(others.text("$.title"));
        assertThat(unknown.text("$.detail")).isEqualTo(others.text("$.detail"));
        assertThat(unknown.text("$.code")).isEqualTo(others.text("$.code"));
        assertThat(unknown.text("$.code")).isEqualTo(NOT_FOUND_CODE);

        // When C1이 주문 번호 abc로 조회한다
        Reply nonNumeric = get(customer(C1), "/orders/abc");

        // Then "없음"(404)을 받는다
        assertThat(nonNumeric.status()).isEqualTo(404);
        assertThat(nonNumeric.text("$.code")).isEqualTo(NOT_FOUND_CODE);
    }

    @Test
    @DisplayName("TC-031 거절된 주문을 조회하면 사유와 상품 목록이 함께 나온다: 재고 부족")
    void rejectedOrderShowsOutOfStockReasonAndShortageProduct() {
        // Given C1이 상품 A 3개와 상품 B 2개를 주문해 "거절"로 기록됐다
        long orderNo = seed(
                C1,
                OrderStatus.REJECTED,
                OrderReason.OUT_OF_STOCK,
                null,
                item("A", 3),
                item("B", 2, OrderReason.OUT_OF_STOCK));

        // When C1이 그 주문을 조회한다
        Reply reply = get(customer(C1), "/orders/" + orderNo);

        // Then 상태 "거절", 사유 "재고 부족", 부족한 상품 B가 나온다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("OUT_OF_STOCK");
        assertThat(reply.texts("$.shortageProductIds")).containsExactly("B");
    }

    @Test
    @DisplayName("TC-031 거절된 주문을 조회하면 사유와 상품 목록이 함께 나온다: 상품 없음")
    void rejectedOrderShowsProductNotFoundReasonAndMissingAndShortageProducts() {
        // Given C1이 상품 A 3개, 상품 Z 1개, 상품 B 2개를 주문해 "거절"로 기록됐다 (Z는 없는 상품, B는 재고 부족)
        long orderNo = seed(
                C1,
                OrderStatus.REJECTED,
                OrderReason.PRODUCT_NOT_FOUND,
                null,
                item("A", 3),
                item("Z", 1, OrderReason.PRODUCT_NOT_FOUND),
                item("B", 2, OrderReason.OUT_OF_STOCK));

        // When C1이 그 주문을 조회한다
        Reply reply = get(customer(C1), "/orders/" + orderNo);

        // Then 상태 "거절", 사유 "상품 없음", 없는 상품 Z와 부족한 상품 B가 모두 나온다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        assertThat(reply.text("$.reason")).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(reply.texts("$.missingProductIds")).containsExactly("Z");
        assertThat(reply.texts("$.shortageProductIds")).containsExactly("B");
    }

    @Test
    @DisplayName("TC-034 고객이 확정 주문을 조회하면 주문 번호, 항목, 상태, 주문 시각이 나온다")
    void customerSeesConfirmedOrderWithItemsAndCreatedAt() {
        // Given C1이 상품 A 3개를 주문해 "확정"으로 기록됐다
        long orderNo = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When C1이 그 주문을 조회한다
        Reply reply = get(customer(C1), "/orders/" + orderNo);

        // Then 주문 번호, 주문 항목(상품 A, 수량 3), 상태 "확정", 주문 시각이 나온다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.orderNo()).isEqualTo(orderNo);
        assertThat(itemsOf(reply)).containsExactly(new Item("A", 3));
        assertThat(reply.text("$.status")).isEqualTo("CONFIRMED");
        String createdAt = reply.text("$.createdAt");
        assertThat(createdAt).isNotBlank();
        // 값은 단언하지 않는다. 글자 모양을 확인 결과로 보고하려고 남긴다
        log.info("createdAt={}", createdAt);
    }
}
