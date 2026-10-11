package com.example.msa.order.client;

import io.github.resilience4j.core.IntervalBiFunction;
import java.time.Duration;
import java.util.List;

/**
 * 재시도 전 대기 일정이다. 설정의 대기 목록을 받아, 몇 번째 재시도인지에 따라 대기 시간을 밀리초로 돌려준다.
 * 기본값은 즉시, 1초, 2초, 4초, 8초다. 첫째 재시도 전에는 첫째 값만큼, 둘째 재시도 전에는 둘째 값만큼 기다린다.
 *
 * <p>Resilience4j Retry에는 {@link #intervalFunction()}으로 넘긴다. Retry는 지금까지 시도한 횟수를 알려 주고 다음 대기를 묻는다.
 * 첫 시도가 실패한 뒤 묻는 횟수는 1이고, 그때의 대기가 첫째 재시도 전 대기다.
 */
public final class RetrySchedule {

    private final List<Duration> waits;

    public RetrySchedule(List<Duration> waits) {
        this.waits = List.copyOf(waits);
    }

    /** {@code retryNumber}번째 재시도 전의 대기 시간(밀리초)이다. 재시도 번호는 1부터 센다. */
    public long waitMillisBeforeRetry(int retryNumber) {
        return waits.get(retryNumber - 1).toMillis();
    }

    /** Resilience4j Retry에 넘기는 간격 함수다. 지금까지 시도한 횟수가 곧 다음 재시도의 번호다. */
    public <T> IntervalBiFunction<T> intervalFunction() {
        return (attempts, result) -> waitMillisBeforeRetry(attempts);
    }
}
