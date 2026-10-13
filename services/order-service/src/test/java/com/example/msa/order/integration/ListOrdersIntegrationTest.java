package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스토리 2: 고객이 자기 주문 목록을 조회한다. 시나리오 2.1~2.6이다.
 *
 * <p>목록 응답의 모양({@code orders}, {@code page}, {@code size}, {@code hasNext})과 오류 {@code code}는 계약 파일
 * specs/002-view-orders/contracts/order-query-api.yaml이 정했다. "주문이 있다"는 상태는 {@code OrderMapper}로 직접 저장해서 만든다.
 */
class ListOrdersIntegrationTest extends ViewTestSupport {

    @Test
    @DisplayName("TC-010 로그인한 사람만 주문하고, 자기 주문만 본다: C1이 자기 주문 목록을 조회하면 C1의 주문만 나온다")
    void customerListContainsOnlyOwnOrders() {
        // Given C1의 주문이 있다
        long c1Order = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When C1이 자기 주문 목록을 조회한다
        Reply reply = get(customer(C1), "/orders");

        // Then C1의 주문만 나온다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).containsExactly(c1Order);
    }

    @Test
    @DisplayName("TC-029 고객의 목록에는 다른 고객의 주문이 섞이지 않는다")
    void listsDoNotMixOtherCustomersOrders() {
        // Given C1에게 주문이 있고, C2에게 주문이 있다
        long c1Order = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));
        long c2Order = seed(C2, OrderStatus.CONFIRMED, null, null, item("A", 1));

        // When C1이 자기 주문 목록을 조회한다
        Reply c1Reply = get(customer(C1), "/orders");

        // Then C1의 주문만 나오고 C2의 주문은 나오지 않는다
        assertThat(c1Reply.status()).isEqualTo(200);
        assertThat(orderNos(c1Reply)).containsExactly(c1Order);

        // When C2가 자기 주문 목록을 조회한다
        Reply c2Reply = get(customer(C2), "/orders");

        // Then C2의 주문만 나오고 C1의 주문은 나오지 않는다
        assertThat(c2Reply.status()).isEqualTo(200);
        assertThat(orderNos(c2Reply)).containsExactly(c2Order);
    }

    @Test
    @DisplayName("TC-028 주문이 하나도 없는 고객의 목록은 비어 있다")
    void customerWithoutOrdersGetsEmptyList() {
        // Given C2의 주문이 하나도 없다 (C1의 주문만 있다)
        seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When C2가 자기 주문 목록을 조회한다
        Reply reply = get(customer(C2), "/orders");

        // Then 오류 없이 빈 목록을 받는다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).isEmpty();
    }

    @Test
    @DisplayName("TC-033 고객은 처리중인 주문과 실패한 주문도 조회한다")
    void customerSeesPendingAndFailedOrders() {
        // Given C1에게 "처리중" 주문과 "실패" 주문이 있다
        long pending = seed(C1, OrderStatus.PENDING, null, null, item("A", 3));
        long failed = seed(C1, OrderStatus.FAILED, OrderReason.RETRY_LATER, null, item("A", 3));

        // When C1이 "처리중" 주문을 조회한다
        Reply pendingReply = get(customer(C1), "/orders/" + pending);

        // Then 상태 "처리중"인 그 주문을 받는다
        assertThat(pendingReply.status()).isEqualTo(200);
        assertThat(pendingReply.text("$.status")).isEqualTo("PENDING");

        // When C1이 "실패" 주문을 조회한다
        Reply failedReply = get(customer(C1), "/orders/" + failed);

        // Then 상태 "실패", 사유 "잠시 후 다시 시도"인 그 주문을 받는다
        assertThat(failedReply.status()).isEqualTo(200);
        assertThat(failedReply.text("$.status")).isEqualTo("FAILED");
        assertThat(failedReply.text("$.reason")).isEqualTo("RETRY_LATER");

        // When C1이 자기 주문 목록을 조회한다
        Reply list = get(customer(C1), "/orders");

        // Then "처리중" 주문과 "실패" 주문이 모두 나온다
        assertThat(list.status()).isEqualTo(200);
        assertThat(orderNos(list)).containsExactlyInAnyOrder(pending, failed);
    }

    @Test
    @DisplayName("TC-030 관리자 역할만 있는 사용자는 고객용 조회로 자기 주문만 본다")
    void adminRoleSeesOnlyOwnOrdersThroughCustomerView() {
        // Given M1의 주문과 C1의 주문이 있다
        long m1Order = seed(M1, OrderStatus.CONFIRMED, null, null, item("A", 1));
        long c1Order = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));

        // When M1이 자기 주문 목록을 조회한다
        Reply list = get(admin(M1), "/orders");

        // Then M1의 주문만 나온다
        assertThat(list.status()).isEqualTo(200);
        assertThat(orderNos(list)).containsExactly(m1Order);

        // When M1이 자기 주문을 조회한다
        Reply own = get(admin(M1), "/orders/" + m1Order);

        // Then 그 주문을 받는다
        assertThat(own.status()).isEqualTo(200);
        assertThat(own.orderNo()).isEqualTo(m1Order);

        // When M1이 C1의 주문을 조회한다
        Reply others = get(admin(M1), "/orders/" + c1Order);

        // Then "없음"(404)을 받는다
        assertThat(others.status()).isEqualTo(404);
    }

    @Test
    @DisplayName("TC-035 주문 목록은 최근 주문이 먼저 나오고 쪽으로 나뉜다")
    void listIsNewestFirstAndPaged() {
        // Given C1에게 주문 25개가 있다 (저장 순서대로 주문 번호가 커진다)
        List<Long> saved = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            saved.add(seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 1)));
        }
        List<Long> newestFirst = new ArrayList<>(saved);
        Collections.reverse(newestFirst);

        // When C1이 쪽 지정 없이 자기 주문 목록을 조회한다
        Reply first = get(customer(C1), "/orders");

        // Then 가장 최근 주문 20개가 최근 순서로 나오고 hasNext는 true다
        assertThat(first.status()).isEqualTo(200);
        assertThat(orderNos(first)).containsExactlyElementsOf(newestFirst.subList(0, 20));
        assertThat(first.text("$.hasNext")).isEqualTo("true");

        // When C1이 page=1로 조회한다
        Reply second = get(customer(C1), "/orders?page=1");

        // Then 나머지 5개(가장 오래된 5개를 큰 순서로)가 나오고 hasNext는 false다
        assertThat(second.status()).isEqualTo(200);
        assertThat(orderNos(second)).containsExactlyElementsOf(newestFirst.subList(20, 25));
        assertThat(second.text("$.hasNext")).isEqualTo("false");

        // When C1이 size=101로 조회한다
        Reply tooBig = get(customer(C1), "/orders?size=101");

        // Then 400을 받는다 (오류 종류는 공통 전제의 INVALID_QUERY_PARAMETER)
        assertThat(tooBig.status()).isEqualTo(400);
        assertThat(tooBig.text("$.code")).isEqualTo("INVALID_QUERY_PARAMETER");
    }
}
