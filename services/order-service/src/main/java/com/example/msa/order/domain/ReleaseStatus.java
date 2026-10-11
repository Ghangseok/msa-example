package com.example.msa.order.domain;

/**
 * 실패한 주문의 재고 해제 결과다. "실패"가 아니거나 해제 요청 전이면 값이 없다. DB에는 이름 문자열로 담는다.
 * 근거는 data-model.md 4-2절이다.
 */
public enum ReleaseStatus {
    /** 해제 불필요. 서킷이 처음부터 열려 있어 예약 요청을 한 번도 보내지 않았다. */
    NOT_REQUIRED,
    /** 해제 완료. 재고가 줄어든 채 남지 않았다. */
    RELEASED,
    /** 해제 실패. 해제 요청이 끝내 실패했다. 오류 로그도 남긴다. */
    RELEASE_FAILED
}
