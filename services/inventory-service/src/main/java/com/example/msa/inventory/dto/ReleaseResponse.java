package com.example.msa.inventory.dto;

import com.example.msa.inventory.domain.ReservationStatus;

/**
 * 예약 해제의 결과다. contracts/inventory-api.yaml의 ReleaseResult와 같다.
 * status는 해제 뒤의 예약 상태다. 거절된 예약은 REJECTED 그대로이고, 나머지는 RELEASED다.
 */
public record ReleaseResponse(long orderNo, ReservationStatus status) {}
