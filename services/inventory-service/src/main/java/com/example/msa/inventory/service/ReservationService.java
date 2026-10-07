package com.example.msa.inventory.service;

import com.example.msa.inventory.dto.ReleaseResponse;
import com.example.msa.inventory.dto.ReservationResponse;
import com.example.msa.inventory.dto.ReserveRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 예약과 해제의 흐름을 조립한다. 이 클래스에는 트랜잭션이 없고, 트랜잭션은 {@link ReservationTxService}가 연다.
 *
 * <p>같은 주문 번호의 요청이 동시에 들어오면 예약 기록의 기본 키에 한쪽만 들어가고, 늦은 쪽은 DuplicateKeyException을 받는다.
 * 늦은 쪽의 트랜잭션은 모두 되돌려졌으므로(재고도 줄지 않았다) 처음부터 한 번 다시 한다.
 * 다시 하면 먼저 들어간 예약 기록이 보여서 저장된 결과를 돌려주거나 충돌로 거부한다. 다시 하는 횟수는 한 번이다.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationTxService transactionService;

    public ReservationService(ReservationTxService transactionService) {
        this.transactionService = transactionService;
    }

    public ReservationResponse reserve(long orderNo, List<ReserveRequest.Item> items) {
        try {
            return transactionService.reserve(orderNo, items);
        } catch (DuplicateKeyException e) {
            log.info("같은 주문 번호의 요청이 동시에 들어와 예약 기록의 기본 키에 걸렸다. 처음부터 한 번 다시 한다. orderNo={}", orderNo);
            return transactionService.reserve(orderNo, items);
        }
    }

    public ReleaseResponse release(long orderNo) {
        try {
            return transactionService.release(orderNo);
        } catch (DuplicateKeyException e) {
            log.info("해제 표식을 넣다 예약 기록의 기본 키에 걸렸다. 처음부터 한 번 다시 한다. orderNo={}", orderNo);
            return transactionService.release(orderNo);
        }
    }
}
