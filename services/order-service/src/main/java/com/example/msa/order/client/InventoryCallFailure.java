package com.example.msa.order.client;

/**
 * 재고 서비스 호출이 업무 결과를 얻지 못하고 끝났다는 알림이다. 재고 서비스가 409나 500을 돌려줬거나(502·503·504 포함),
 * 연결이 실패했거나, 시도 제한 시간이 지났거나, 서킷이 열려 있어서 호출하지 않은 경우다.
 * 부르는 쪽은 이 예외를 받으면 주문을 "실패"로 기록한다.
 */
public class InventoryCallFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 실패의 종류다. */
    public enum Kind {
        /** 서킷이 열려 있어 요청을 보내지 않았다. */
        CIRCUIT_OPEN,
        /** 연결하지 못했거나 연결이 끊겼다. */
        CONNECTION_FAILURE,
        /** 시도 제한 시간이 지났다. */
        TIMEOUT,
        /** 재고 서비스가 2xx가 아닌 상태 코드를 돌려줬다. 상태 코드는 {@link #httpStatus()}에 있다. */
        HTTP_STATUS,
        /** 그 밖에 예상하지 못한 오류다. 예를 들어 응답 본문을 읽지 못했다. */
        UNEXPECTED
    }

    private final Kind kind;
    private final int httpStatus;

    public InventoryCallFailure(Kind kind, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public Kind kind() {
        return kind;
    }

    /** 재고 서비스가 돌려준 상태 코드다. 응답을 받지 못한 실패에서는 0이다. */
    public int httpStatus() {
        return httpStatus;
    }
}
