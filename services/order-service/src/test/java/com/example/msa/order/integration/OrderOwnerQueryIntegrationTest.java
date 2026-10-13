package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 받치는 테스트다. 인수 시나리오를 직접 검증하지 않아서 테스트 케이스 번호를 붙이지 않는다.
 * 기대값은 docs/requirements/uc-002-view-orders.md 3절 "업무 규칙"의 첫 문장이다: 고객은 다른 고객의 주문을 볼 수 없다.
 */
class OrderOwnerQueryIntegrationTest extends ViewTestSupport {

    @Test
    @DisplayName("쿼리에 다른 고객의 ID를 붙여도 무시하고 토큰의 고객 주문만 나온다")
    void customerIdInQueryIsIgnored() {
        // Given C1의 주문과 C2의 주문이 있다
        long c1Order = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));
        seed(C2, OrderStatus.CONFIRMED, null, null, item("A", 1));

        // When C1의 토큰으로 쿼리에 C2의 sub를 붙여 부른다
        Reply reply = get(customer(C1), "/orders?customerId=" + C2);

        // Then 결과에 C1의 주문만 있다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).containsExactly(c1Order);
    }
}
