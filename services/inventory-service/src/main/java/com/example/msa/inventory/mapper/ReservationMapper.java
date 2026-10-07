package com.example.msa.inventory.mapper;

import com.example.msa.inventory.domain.ReservationReason;
import com.example.msa.inventory.domain.ReservationStatus;
import com.example.msa.inventory.dto.ReservationWithItems;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 예약 기록과 그 항목을 다루는 Mapper다. SQL은 resources/mapper/ReservationMapper.xml에 있다. */
@Mapper
public interface ReservationMapper {

    /** 예약 기록과 항목을 상품 ID 순으로 한 번에 읽는다. 예약 기록이 없으면 null이다. 항목이 없는 기록은 items가 빈 목록이다. */
    ReservationWithItems selectReservationWithItems(@Param("orderNo") long orderNo);

    /** 예약 기록 한 줄을 잠그고 상태를 읽는다(SELECT ... FOR UPDATE). 예약 기록이 없으면 null이다. */
    ReservationStatus selectStatusForUpdate(@Param("orderNo") long orderNo);

    /** 예약 기록을 넣는다. 같은 주문 번호가 이미 있으면 고유 제약 위반이다. reason은 거절일 때만 값이 있다. */
    int insertReservation(
            @Param("orderNo") long orderNo,
            @Param("status") ReservationStatus status,
            @Param("reason") ReservationReason reason);

    /** 예약 항목을 넣는다. result는 거절된 예약에서 그 상품이 부족했거나 없었을 때만 값이 있다. */
    int insertReservationItem(
            @Param("orderNo") long orderNo,
            @Param("productId") String productId,
            @Param("quantity") int quantity,
            @Param("result") ReservationReason result);

    /** 예약 기록의 상태를 바꾼다. */
    int updateReservationStatus(@Param("orderNo") long orderNo, @Param("status") ReservationStatus status);
}
