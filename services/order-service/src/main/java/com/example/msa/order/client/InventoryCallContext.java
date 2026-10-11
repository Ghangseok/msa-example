package com.example.msa.order.client;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재고 호출 한 번(예약 하나, 해제 하나)마다 만드는 작은 객체다. 서킷이 허가해서 실제로 요청을 보낸 시도의 수를 센다.
 *
 * <p>"예약 요청을 보낸 적이 있는가"는 이 객체가 센다. 서킷 상태를 미리 따로 묻지 않는다. 미리 물으면 반쯤 열린 상태에서
 * 시험 호출 허가를 하나 더 써 버리기 때문이다. 근거는 data-model.md 6-1절 마지막 문단이다.
 */
public final class InventoryCallContext {

    private final AtomicInteger sentAttempts = new AtomicInteger();

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
}
