package com.example.msa.order.service;

import com.example.msa.order.client.InventoryCallContext;
import com.example.msa.order.client.InventoryCallFailure;
import com.example.msa.order.client.InventoryClient;
import com.example.msa.order.client.ReservationResult;
import com.example.msa.order.domain.ReleaseStatus;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/**
 * 실패한 주문의 재고 예약 해제를 고객 응답과 따로 돌린다. {@code releaseExecutor}에서 실행한다.
 * {@link OrderService}가 "실패" 결과를 저장한 트랜잭션이 커밋된 뒤에 맡기고 바로 응답한다.
 * 같은 클래스 안에서 비동기 메서드를 부르면 Spring 프록시를 거치지 않으므로 별도 빈에 둔다.
 *
 * <p>해제 작업은 {@link InventoryClient#release}를 한 번 부른다. 결과가 RELEASED나 REJECTED면 "해제 완료"로, 끝내 실패하면
 * "해제 실패"로 새 트랜잭션에 저장하고 오류 로그를 남긴다. 해제까지 실패하면 관리자 조회로 찾는다(도메인 분석 4절).
 */
@Service
public class ReservationReleaser {

    private static final Logger log = LoggerFactory.getLogger(ReservationReleaser.class);

    private final Executor releaseExecutor;
    private final InventoryClient inventoryClient;
    private final OrderTxService orderTxService;

    public ReservationReleaser(
            @Qualifier("releaseExecutor") Executor releaseExecutor,
            InventoryClient inventoryClient,
            OrderTxService orderTxService) {
        this.releaseExecutor = releaseExecutor;
        this.inventoryClient = inventoryClient;
        this.orderTxService = orderTxService;
    }

    /** 해제를 해제 실행기에 맡기고 바로 돌아온다. 대기열이 가득 차면 해제하지 못한 것으로 기록한다. */
    public void releaseLater(long orderNo) {
        try {
            releaseExecutor.execute(() -> release(orderNo));
        } catch (TaskRejectedException e) {
            log.error("해제 대기열이 가득 차서 해제를 맡기지 못했다. orderNo={}", orderNo, e);
            saveReleaseStatus(orderNo, ReleaseStatus.RELEASE_FAILED);
        }
    }

    private void release(long orderNo) {
        ReleaseStatus releaseStatus;
        try {
            InventoryClient.ReleaseResult result = inventoryClient.release(orderNo, new InventoryCallContext());
            // 거절된 예약의 해제는 REJECTED를 그대로 돌려준다. 어느 쪽이든 재고가 줄어든 채 남지 않았다.
            boolean stockIsNotHeld = result.status() == ReservationResult.Status.RELEASED
                    || result.status() == ReservationResult.Status.REJECTED;
            releaseStatus = stockIsNotHeld ? ReleaseStatus.RELEASED : ReleaseStatus.RELEASE_FAILED;
        } catch (InventoryCallFailure failure) {
            log.error(
                    "예약 해제에 끝내 실패했다. 관리자 조회로 찾는다. orderNo={} kind={} httpStatus={}",
                    orderNo,
                    failure.kind(),
                    failure.httpStatus());
            releaseStatus = ReleaseStatus.RELEASE_FAILED;
        }
        saveReleaseStatus(orderNo, releaseStatus);
    }

    private void saveReleaseStatus(long orderNo, ReleaseStatus releaseStatus) {
        try {
            orderTxService.applyReleaseResult(orderNo, releaseStatus);
        } catch (RuntimeException e) {
            log.error("해제 결과를 주문에 저장하지 못했다. orderNo={} releaseStatus={}", orderNo, releaseStatus, e);
        }
    }
}
