package com.example.msa.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 쪽 지정 규칙을 본다. 기대값은 테스트 케이스 문서 공통 전제의 "page(0부터)와 size(기본 20, 최대 100), 범위 밖의 값은 400"과
 * 시나리오 2.6(둘째 쪽은 앞의 20개 다음부터)이다. size의 아래 한계와 정수가 아닌 값은 문서에 기대값이 없어서 단언하지 않는다.
 */
class PageRequestTest {

    @Test
    @DisplayName("page와 size를 주지 않으면 page는 0, size는 20이다")
    void defaults() {
        PageRequest request = PageRequest.of(null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("size=100은 맞고 size=101은 틀리다")
    void sizeUpperLimitIs100() {
        assertThat(PageRequest.of(null, "100").size()).isEqualTo(100);

        assertThatThrownBy(() -> PageRequest.of(null, "101"))
                .isInstanceOfSatisfying(
                        InvalidQueryException.class,
                        e -> assertThat(e.violations())
                                .extracting(InvalidQueryException.Violation::field)
                                .containsExactly("size"));
    }

    @Test
    @DisplayName("page는 0부터이므로 page=-1은 틀리다")
    void negativePageIsInvalid() {
        assertThatThrownBy(() -> PageRequest.of("-1", null))
                .isInstanceOfSatisfying(
                        InvalidQueryException.class,
                        e -> assertThat(e.violations())
                                .extracting(InvalidQueryException.Violation::field)
                                .containsExactly("page"));
    }

    @Test
    @DisplayName("page=1, size=20이면 앞의 20개를 건너뛴다(offset 20)")
    void offsetSkipsPreviousPages() {
        assertThat(PageRequest.of("1", "20").offset()).isEqualTo(20);
    }

    @Test
    @DisplayName("page=2147483647, size=100이어도 offset이 음수로 넘치지 않는다")
    void offsetDoesNotOverflow() {
        assertThat(PageRequest.of("2147483647", "100").offset()).isEqualTo(214_748_364_700L);
    }
}
