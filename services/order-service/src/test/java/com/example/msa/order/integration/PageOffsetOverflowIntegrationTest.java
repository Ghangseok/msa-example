package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 아주 큰 {@code page}를 준 목록 조회다. 계약 파일 specs/002-view-orders/contracts/order-query-api.yaml은 {@code page}를
 * "0 이상의 int32"로 정했고, 테스트 케이스 문서의 공통 전제는 범위 밖의 값만 400이라고 했다. int32 안의 값은 범위 안이므로 400이 아니다.
 * 그 쪽에 주문이 없으면 빈 목록이라는 기대값은 문서에 직접 적혀 있지 않다. 계약과 공통 전제에서 이어 낸 값이라 [제안]이다.
 */
class PageOffsetOverflowIntegrationTest extends ViewTestSupport {

    private static final String LAST_PAGE = "page=2147483647&size=100";

    @Test
    @DisplayName("고객 목록: page가 int32 최대값이어도 500이 아니라 빈 목록을 받는다")
    void customerListWithHugePageIsEmpty() {
        // Given C1의 주문이 하나 있다
        seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When C1이 page=2147483647, size=100으로 조회한다
        Reply reply = get(customer(C1), "/orders?" + LAST_PAGE);

        // Then 200이고 주문 없이 hasNext가 false다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).isEmpty();
        assertThat(reply.text("$.hasNext")).isEqualTo("false");
    }

    @Test
    @DisplayName("관리자 목록: page가 int32 최대값이어도 500이 아니라 빈 목록을 받는다")
    void adminListWithHugePageIsEmpty() {
        // Given C1의 주문이 하나 있다
        seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When M1이 page=2147483647, size=100으로 관리자 조회를 한다
        Reply reply = get(admin(M1), "/admin/orders?" + LAST_PAGE);

        // Then 200이고 주문 없이 hasNext가 false다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).isEmpty();
        assertThat(reply.text("$.hasNext")).isEqualTo("false");
    }
}
