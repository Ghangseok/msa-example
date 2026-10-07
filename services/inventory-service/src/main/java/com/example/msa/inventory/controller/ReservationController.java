package com.example.msa.inventory.controller;

import com.example.msa.inventory.dto.ReleaseResponse;
import com.example.msa.inventory.dto.ReservationResponse;
import com.example.msa.inventory.dto.ReserveRequest;
import com.example.msa.inventory.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 서비스만 부르는 내부 API다. 요청 형식 검사, 서비스 호출, 응답 변환만 한다. 업무 판단은 하지 않는다.
 * 업무 결과(예약됨, 거절됨, 해제됨)는 처음 요청이든 재요청이든 언제나 200이다. 기준은 contracts/inventory-api.yaml이다.
 */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 주문 항목 전체의 재고를 한꺼번에 예약한다. 형식이 틀리면 400, 같은 주문 번호에 다른 내용이면 409다. */
    @PutMapping("/{orderNo}")
    public ReservationResponse reserve(
            @PathVariable @Positive long orderNo, @RequestBody @Valid ReserveRequest request) {
        return reservationService.reserve(orderNo, request.items());
    }

    /** 예약을 해제해 줄였던 재고를 되돌린다. 예약 기록이 없으면 해제 표식만 남긴다. */
    @DeleteMapping("/{orderNo}")
    public ReleaseResponse release(@PathVariable @Positive long orderNo) {
        return reservationService.release(orderNo);
    }
}
