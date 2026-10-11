package com.example.msa.order.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재고 호출 한 번(예약 하나, 해제 하나)마다 만드는 작은 객체다. 시작 시각과, 서킷이 허가해서 실제로 요청을 보낸 시도의 수를 센다.
 *
 * <p>"예약 요청을 보낸 적이 있는가"는 이 객체가 센다. 서킷 상태를 미리 따로 묻지 않는다. 미리 물으면 반쯤 열린 상태에서
 * 시험 호출 허가를 하나 더 써 버리기 때문이다. 근거는 data-model.md 6-1절 마지막 문단이다.
 *
 * <p>재시도를 포함한 전체 한도(기본 30초)도 이 객체가 판단한다. 한도는 이 객체를 만든 시각부터 잰다.
 * 30초가 되기 전이면 남은 시간이 시도당 제한 시간보다 짧아도 시도를 시작하고, 시도는 30초에 끊는다
 * (테스트 케이스 문서 공통 전제). 재시도는 전체 한도 안에서만 하므로, 한도 없이 만든 문맥은 재시도를 허락하지 않는다.
 * 그런 문맥으로 부르면 시도 하나만 보낸다.
 */
public final class InventoryCallContext {

    /** JDK HttpClient의 요청 제한 시간은 0보다 커야 한다. 남은 시간이 없을 때도 이 값을 준다. */
    private static final Duration MINIMUM_ATTEMPT_TIMEOUT = Duration.ofMillis(1);

    private final AtomicInteger sentAttempts = new AtomicInteger();
    private final Clock clock;
    private final Instant startedAt;
    private final Duration overallLimit;

    /** 전체 한도가 없는 문맥이다. 시도 하나만 보내고 재시도하지 않는다. */
    public InventoryCallContext() {
        this(Clock.systemUTC(), null);
    }

    /**
     * 전체 한도가 있는 문맥이다. 지금 시각이 시작 시각이다.
     *
     * @param clock 지금 시각을 읽는 시계. 테스트에서는 시각을 정하는 시계를 넣는다
     * @param overallLimit 재시도를 포함한 전체 한도. null이면 한도가 없는 문맥이다
     */
    public InventoryCallContext(Clock clock, Duration overallLimit) {
        this.clock = clock;
        this.startedAt = clock.instant();
        this.overallLimit = overallLimit;
    }

    /** 시도를 하나 보냈다고 기록하고, 몇 번째 시도인지 돌려준다. {@link InventoryClient}가 서킷의 허가를 받은 직후에 부른다. */
    int recordSentAttempt() {
        return sentAttempts.incrementAndGet();
    }

    /** 서킷이 허가해 실제로 요청을 보낸 시도의 수다. */
    public int sentAttempts() {
        return sentAttempts.get();
    }

    /** 요청을 한 번이라도 보냈는가. 보냈으면 재고 서비스가 예약을 처리했을 수 있으므로 해제가 필요하다. */
    public boolean requestWasSent() {
        return sentAttempts.get() > 0;
    }

    /** 이 문맥이 재시도를 허락하는가. 전체 한도가 있는 문맥만 허락한다. */
    boolean retriesAllowed() {
        return overallLimit != null;
    }

    /**
     * {@code wait}만큼 기다린 뒤 다음 시도를 시작해도 되는가. "지금 시각 + 대기"가 "시작 시각 + 전체 한도"보다 앞서야 한다.
     * 정확히 한도가 되는 때에 시작하게 되면 허락하지 않는다. "30초가 되기 전"이 아니기 때문이다.
     */
    public boolean allowsRetryAfterWaiting(Duration wait) {
        if (overallLimit == null) {
            return false;
        }
        return clock.instant().plus(wait).isBefore(startedAt.plus(overallLimit));
    }

    /**
     * 다음 시도에 줄 제한 시간이다. 시도당 제한 시간과 전체 한도까지 남은 시간 가운데 짧은 쪽이다.
     * 전체 한도가 없으면 시도당 제한 시간을 그대로 준다.
     */
    public Duration timeoutForNextAttempt(Duration attemptTimeout) {
        if (overallLimit == null) {
            return attemptTimeout;
        }
        Duration remaining = Duration.between(clock.instant(), startedAt.plus(overallLimit));
        Duration shorter = remaining.compareTo(attemptTimeout) < 0 ? remaining : attemptTimeout;
        return shorter.compareTo(MINIMUM_ATTEMPT_TIMEOUT) < 0 ? MINIMUM_ATTEMPT_TIMEOUT : shorter;
    }
}
