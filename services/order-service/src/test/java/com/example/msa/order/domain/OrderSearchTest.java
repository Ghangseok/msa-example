package com.example.msa.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 관리자 조회 조건의 규칙을 본다. 기대값은 시나리오 4.3(조건은 생략할 수 있고, 없는 상태 이름과 없는 해제 결과 값은 거부한다)과
 * 001이 만든 {@link OrderStatus}, {@link ReleaseStatus}의 값이다. 소문자 이름과 빈 글자는 문서에 기대값이 없어서 단언하지 않는다.
 */
class OrderSearchTest {

    @Test
    @DisplayName("조건을 둘 다 주지 않으면 조건 없음이다")
    void noConditions() {
        OrderSearch search = OrderSearch.of(null, null);

        assertThat(search.status()).isNull();
        assertThat(search.releaseStatus()).isNull();
    }

    @Test
    @DisplayName("상태 FAILED와 해제 결과 RELEASE_FAILED는 맞는 값이다")
    void validConditions() {
        OrderSearch search = OrderSearch.of("FAILED", "RELEASE_FAILED");

        assertThat(search.status()).isEqualTo(OrderStatus.FAILED);
        assertThat(search.releaseStatus()).isEqualTo(ReleaseStatus.RELEASE_FAILED);
    }

    @Test
    @DisplayName("상태 네 개와 해제 결과 세 개가 모두 맞는 값이다")
    void everyStatusAndReleaseStatusIsValid() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(OrderSearch.of(status.name(), null).status()).isEqualTo(status);
        }
        for (ReleaseStatus releaseStatus : ReleaseStatus.values()) {
            assertThat(OrderSearch.of(null, releaseStatus.name()).releaseStatus())
                    .isEqualTo(releaseStatus);
        }
    }

    @Test
    @DisplayName("없는 상태 이름 UNKNOWN은 틀리다. 틀린 매개변수 이름은 status다")
    void unknownStatusIsInvalid() {
        assertThatThrownBy(() -> OrderSearch.of("UNKNOWN", null))
                .isInstanceOfSatisfying(
                        InvalidQueryException.class,
                        e -> assertThat(e.violations())
                                .extracting(InvalidQueryException.Violation::field)
                                .containsExactly("status"));
    }

    @Test
    @DisplayName("없는 해제 결과 값 UNKNOWN은 틀리다. 틀린 매개변수 이름은 releaseStatus다")
    void unknownReleaseStatusIsInvalid() {
        assertThatThrownBy(() -> OrderSearch.of(null, "UNKNOWN"))
                .isInstanceOfSatisfying(
                        InvalidQueryException.class,
                        e -> assertThat(e.violations())
                                .extracting(InvalidQueryException.Violation::field)
                                .containsExactly("releaseStatus"));
    }
}
