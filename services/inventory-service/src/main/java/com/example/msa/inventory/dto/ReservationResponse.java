package com.example.msa.inventory.dto;

import com.example.msa.inventory.domain.ReservationReason;
import com.example.msa.inventory.domain.ReservationStatus;
import java.util.List;

/**
 * 예약 요청의 업무 결과다. contracts/inventory-api.yaml의 ReservationResult와 같다.
 * 처음 요청이든 재요청이든 같은 모양이다. reason은 거절됐을 때만 값이 있다.
 *
 * @param shortageProductIds 재고가 부족한 상품 ID. 상품 ID 순. 없으면 빈 목록
 * @param missingProductIds 재고 데이터에 없는 상품 ID. 상품 ID 순. 없으면 빈 목록
 */
public record ReservationResponse(
        long orderNo,
        ReservationStatus status,
        ReservationReason reason,
        List<String> shortageProductIds,
        List<String> missingProductIds) {}
