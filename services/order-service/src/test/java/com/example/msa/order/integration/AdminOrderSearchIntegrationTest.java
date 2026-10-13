package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스토리 4: 관리자가 상태와 재고 해제 결과로 주문을 조회한다. 시나리오 4.1~4.4다.
 *
 * <p>응답 필드 이름과 값({@code releaseStatus}의 {@code RELEASE_FAILED} 같은 영어 이름)은 계약 파일
 * specs/002-view-orders/contracts/order-query-api.yaml이 정했다. 테스트 케이스 문서는 한국어 개념만 적었다.
 * 관리자 흉내({@code admin(M1)})는 역할 변환기를 거치지 않는다. 변환기는 {@code RealmRoleJwtConverterTest}가 확인한다.
 * 응답에 주문한 고객의 ID가 있는지, 쿼리 값의 대소문자와 빈 값은 문서에 기대값이 없어서 단언하지 않는다.
 */
class AdminOrderSearchIntegrationTest extends ViewTestSupport {

    private static final String INVALID_QUERY_CODE = "INVALID_QUERY_PARAMETER";

    @Test
    @DisplayName("TC-010 로그인한 사람만 주문하고, 자기 주문만 본다: C1이 관리자 조회를 요청하면 403이다")
    void customerCannotUseAdminSearch() {
        // Given 관리자 권한이 없는 C1이 있다 (조회할 주문은 필요 없다)

        // When C1이 관리자 조회를 요청한다
        Reply reply = get(customer(C1), "/admin/orders");

        // Then 403을 받는다 (오류 응답 본문은 Problem Details 형식이다. 공통 전제)
        assertThat(reply.status()).isEqualTo(403);
        assertThat(reply.contentType()).startsWith("application/problem+json");
        assertThat(reply.text("$.code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("TC-010 로그인한 사람만 주문하고, 자기 주문만 본다: M1이 상태 \"실패\"로 조회하면 C1과 C2의 실패 주문이 해제 결과와 함께 나온다")
    void adminSeesAllCustomersFailedOrdersWithReleaseStatus() {
        // Given C1과 C2에게 각각 "실패" 주문이 있다 (해제 결과는 서로 다르다)
        long c1Failed =
                seed(C1, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASE_FAILED, item("A", 3));
        long c2Failed = seed(C2, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASED, item("A", 1));

        // When M1이 상태 "실패"로 관리자 조회를 한다
        Reply reply = get(admin(M1), "/admin/orders?status=FAILED");

        // Then C1과 C2의 실패 주문이 모두 나오고, 각 주문에 해제 결과가 함께 있다
        assertThat(reply.status()).isEqualTo(200);
        assertThat(orderNos(reply)).containsExactlyInAnyOrder(c1Failed, c2Failed);
        assertThat(releaseStatusOf(reply, c1Failed)).isEqualTo("RELEASE_FAILED");
        assertThat(releaseStatusOf(reply, c2Failed)).isEqualTo("RELEASED");
    }

    @Test
    @DisplayName("TC-027 관리자 조회의 조건은 생략할 수 있고, 둘을 함께 주면 모두 맞아야 하며, 잘못된 값은 거부한다")
    void adminConditionsAreOptionalCombinedAndValidated() {
        // Given C1의 "확정" 주문, C1의 "실패" 주문(해제 완료), C2의 "실패" 주문(해제 실패)이 있다
        long c1Confirmed = seed(C1, OrderStatus.CONFIRMED, null, null, item("A", 3));
        long c1Failed = seed(C1, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASED, item("A", 3));
        long c2Failed =
                seed(C2, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASE_FAILED, item("A", 1));

        // When M1이 조건 없이 관리자 조회를 한다
        Reply none = get(admin(M1), "/admin/orders");

        // Then 세 주문이 모두 나온다
        assertThat(none.status()).isEqualTo(200);
        assertThat(orderNos(none)).containsExactlyInAnyOrder(c1Confirmed, c1Failed, c2Failed);

        // When M1이 상태 "실패"와 해제 결과 "해제 실패"를 함께 주고 관리자 조회를 한다
        Reply both = get(admin(M1), "/admin/orders?status=FAILED&releaseStatus=RELEASE_FAILED");

        // Then C2의 "실패" 주문만 나온다
        assertThat(both.status()).isEqualTo(200);
        assertThat(orderNos(both)).containsExactly(c2Failed);

        // When M1이 해제 결과 "해제 실패"만 주고 관리자 조회를 한다
        Reply releaseOnly = get(admin(M1), "/admin/orders?releaseStatus=RELEASE_FAILED");

        // Then C2의 "실패" 주문만 나온다
        assertThat(releaseOnly.status()).isEqualTo(200);
        assertThat(orderNos(releaseOnly)).containsExactly(c2Failed);

        // When M1이 없는 상태 이름으로 관리자 조회를 한다
        Reply unknownStatus = get(admin(M1), "/admin/orders?status=UNKNOWN");

        // Then 400을 받는다
        assertThat(unknownStatus.status()).isEqualTo(400);
        assertThat(unknownStatus.text("$.code")).isEqualTo(INVALID_QUERY_CODE);

        // When M1이 없는 해제 결과 값으로 관리자 조회를 한다
        Reply unknownRelease = get(admin(M1), "/admin/orders?releaseStatus=UNKNOWN");

        // Then 400을 받는다
        assertThat(unknownRelease.status()).isEqualTo(400);
        assertThat(unknownRelease.text("$.code")).isEqualTo(INVALID_QUERY_CODE);
    }

    @Test
    @DisplayName("TC-032 관리자는 해제 실패 주문, 해제 결과가 비어 있는 주문, 처리중 주문을 찾는다")
    void adminFindsReleaseFailedEmptyReleaseAndPendingOrders() {
        // Given C1의 "실패" 주문(해제 실패), C2의 "실패" 주문(해제 완료), C2의 "실패" 주문(해제 결과 비어 있음), C1의 "처리중" 주문이 있다
        long c1ReleaseFailed =
                seed(C1, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASE_FAILED, item("A", 3));
        long c2Released = seed(C2, OrderStatus.FAILED, OrderReason.RETRY_LATER, ReleaseStatus.RELEASED, item("A", 1));
        long c2NoRelease = seed(C2, OrderStatus.FAILED, OrderReason.RETRY_LATER, null, item("A", 2));
        long c1Pending = seed(C1, OrderStatus.PENDING, null, null, item("A", 1));

        // When M1이 해제 결과 "해제 실패"로 관리자 조회를 한다
        Reply releaseFailed = get(admin(M1), "/admin/orders?releaseStatus=RELEASE_FAILED");

        // Then C1의 해제 실패 주문만 나온다
        assertThat(releaseFailed.status()).isEqualTo(200);
        assertThat(orderNos(releaseFailed)).containsExactly(c1ReleaseFailed);

        // When M1이 상태 "실패"로 관리자 조회를 한다
        Reply failed = get(admin(M1), "/admin/orders?status=FAILED");

        // Then "실패" 주문 세 개가 나오고, 해제 결과가 비어 있는 주문은 비어 있는 채로(필드는 있고 값은 null) 나온다
        assertThat(failed.status()).isEqualTo(200);
        assertThat(orderNos(failed)).containsExactlyInAnyOrder(c1ReleaseFailed, c2Released, c2NoRelease);
        Map<?, ?> empty = orderOf(failed, c2NoRelease);
        assertThat(empty.containsKey("releaseStatus")).isTrue();
        assertThat(empty.get("releaseStatus")).isNull();

        // When M1이 상태 "처리중"으로 관리자 조회를 한다
        Reply pending = get(admin(M1), "/admin/orders?status=PENDING");

        // Then C1의 "처리중" 주문만 나온다
        assertThat(pending.status()).isEqualTo(200);
        assertThat(orderNos(pending)).containsExactly(c1Pending);
    }

    /** 목록 응답에서 주문 번호가 {@code orderNo}인 주문 하나를 읽는다. 없으면 실패한다. */
    private static Map<?, ?> orderOf(Reply reply, long orderNo) {
        List<?> found = JsonPath.read(reply.body(), "$.orders[?(@.orderNo==" + orderNo + ")]");
        assertThat(found).hasSize(1);
        return (Map<?, ?>) found.get(0);
    }

    private static String releaseStatusOf(Reply reply, long orderNo) {
        Object value = orderOf(reply, orderNo).get("releaseStatus");
        return value == null ? null : value.toString();
    }
}
