package com.example.msa.order.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 재시도를 포함한 전체 한도(30초) 판단을 본다. Spring 앱을 띄우지 않고, 주입한 시계로 시각을 정한다 [제안].
 *
 * <p>기대값의 근거는 두 가지다.
 *
 * <ul>
 *   <li>테스트 케이스 문서(docs/test-cases/order-placement.md) 공통 전제: "30초가 되기 전이면, 남은 시간이 시도당 제한
 *       시간(2.5초)보다 짧아도 시도를 시작한다. 시도는 30초에 끊는다."
 *   <li>비기능 요구사항 문서(docs/requirements/non-functional.md)의 "장애 전파 차단" 목표값: "재시도 포함 전체 30초, 시도당 2.5초,
 *       재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤)".
 * </ul>
 *
 * 한도 판단은 인수 시나리오 테스트(시나리오 9.1)를 받치는 테스트라서 테스트 케이스 번호를 붙이지 않는다.
 */
class InventoryCallContextTest {

    private static final Duration OVERALL_LIMIT = Duration.ofSeconds(30);
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofMillis(2500);

    @Test
    @DisplayName("다음 시도가 30초가 되기 전에 시작하면 재시도를 허락한다")
    void retryIsAllowedWhenTheNextAttemptStartsBeforeTheLimit() {
        MutableClock clock = new MutableClock();
        InventoryCallContext context = new InventoryCallContext(clock, OVERALL_LIMIT);

        // 다섯째 시도가 시간 초과로 19.5초에 끝났고, 8초를 기다리면 여섯째 시도가 27.5초에 시작한다.
        clock.setElapsed(Duration.ofMillis(19_500));
        assertThat(context.allowsRetryAfterWaiting(Duration.ofSeconds(8))).isTrue();

        // 남은 시간이 시도당 제한 시간(2.5초)보다 짧아도 30초가 되기 전이면 시도를 시작한다.
        clock.setElapsed(Duration.ofMillis(21_500));
        assertThat(context.allowsRetryAfterWaiting(Duration.ofSeconds(8))).isTrue();
    }

    @Test
    @DisplayName("다음 시도가 30초가 되는 때나 그 뒤에 시작하게 되면 재시도를 막는다")
    void retryIsBlockedWhenTheNextAttemptWouldStartAtOrAfterTheLimit() {
        MutableClock clock = new MutableClock();
        InventoryCallContext context = new InventoryCallContext(clock, OVERALL_LIMIT);

        clock.setElapsed(Duration.ofSeconds(22));
        assertThat(context.allowsRetryAfterWaiting(Duration.ofSeconds(8))).isFalse();

        clock.setElapsed(Duration.ofSeconds(25));
        assertThat(context.allowsRetryAfterWaiting(Duration.ofSeconds(8))).isFalse();
    }

    @Test
    @DisplayName("시도에 주는 제한 시간은 시도당 제한 시간과 전체 한도까지 남은 시간 가운데 짧은 쪽이다")
    void attemptTimeoutIsTheShorterOfTheAttemptLimitAndTheRemainingTime() {
        MutableClock clock = new MutableClock();
        InventoryCallContext context = new InventoryCallContext(clock, OVERALL_LIMIT);

        // 남은 시간이 충분하면 시도당 제한 시간 2.5초다.
        assertThat(context.timeoutForNextAttempt(ATTEMPT_TIMEOUT)).isEqualTo(ATTEMPT_TIMEOUT);
        clock.setElapsed(Duration.ofMillis(27_500));
        assertThat(context.timeoutForNextAttempt(ATTEMPT_TIMEOUT)).isEqualTo(ATTEMPT_TIMEOUT);

        // 남은 시간이 시도당 제한 시간보다 짧으면 남은 시간이다. 시도는 30초에 끊는다.
        clock.setElapsed(Duration.ofMillis(28_500));
        assertThat(context.timeoutForNextAttempt(ATTEMPT_TIMEOUT)).isEqualTo(Duration.ofMillis(1_500));
    }

    @Test
    @DisplayName("전체 한도 없이 만든 문맥은 재시도를 허락하지 않고, 시도에 시도당 제한 시간을 그대로 준다")
    void contextWithoutLimitSendsOneAttemptWithTheAttemptTimeout() {
        InventoryCallContext context = new InventoryCallContext();

        assertThat(context.allowsRetryAfterWaiting(Duration.ZERO)).isFalse();
        assertThat(context.timeoutForNextAttempt(ATTEMPT_TIMEOUT)).isEqualTo(ATTEMPT_TIMEOUT);
    }

    /** 시작 시각에서 얼마나 지났는지 테스트가 정하는 시계다. */
    private static final class MutableClock extends Clock {

        private static final Instant START = Instant.parse("2026-10-11T00:00:00Z");

        private Instant now = START;

        void setElapsed(Duration elapsed) {
            now = START.plus(elapsed);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
