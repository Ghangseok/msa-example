package com.example.msa.inventory.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.example.msa.inventory.OracleTestContainer;
import com.example.msa.inventory.domain.ReservationStatus;
import com.example.msa.inventory.dto.ReservationWithItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

/**
 * 예약 기록의 SQL이 실제 Oracle에서 맞게 도는지 본다. 테스트 하나가 끝나면 변경은 롤백된다.
 *
 * <p>기대값의 출처는 도메인 분석 6절 표의 2번("예약 기록의 주문 번호 고유 제약 때문에 한쪽만 반영된다")과
 * 6번("해제 표식(해제됨)만 남긴다"), 설계 문서 6절 "예약 처리" 4번이다.
 * 예약 기록과 항목을 {@code <collection>}으로 한 번에 읽는 것은 코딩 규약 3-2절이다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(OracleTestContainer.class)
@ActiveProfiles("test")
class ReservationMapperTest {

    @Autowired
    private ReservationMapper reservationMapper;

    @Test
    @DisplayName("주문 번호 1001의 예약 기록과 항목을 상품 ID 순으로 한 번에 읽는다")
    void readsReservationWithItemsAtOnce() {
        reservationMapper.insertReservation(1001, ReservationStatus.RESERVED, null);
        // 일부러 B를 먼저 넣어서, 읽을 때 상품 ID 순으로 나오는지 본다
        reservationMapper.insertReservationItem(1001, "B", 2, null);
        reservationMapper.insertReservationItem(1001, "A", 3, null);

        ReservationWithItems reservation = reservationMapper.selectReservationWithItems(1001);

        assertThat(reservation.getOrderNo()).isEqualTo(1001);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.getReason()).isNull();
        assertThat(reservation.getItems())
                .extracting(
                        ReservationWithItems.StoredItem::productId,
                        ReservationWithItems.StoredItem::quantity,
                        ReservationWithItems.StoredItem::result)
                .containsExactly(tuple("A", 3, null), tuple("B", 2, null));
    }

    @Test
    @DisplayName("같은 주문 번호를 한 번 더 넣으면 고유 제약 위반 예외가 난다")
    void secondInsertOfSameOrderNumberViolatesUniqueKey() {
        reservationMapper.insertReservation(1001, ReservationStatus.RESERVED, null);

        assertThatThrownBy(() -> reservationMapper.insertReservation(1001, ReservationStatus.RESERVED, null))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("주문 번호 1002의 해제 표식(해제됨, 항목 없음)을 넣고 읽는다")
    void readsReleaseMarkerWithoutItems() {
        reservationMapper.insertReservation(1002, ReservationStatus.RELEASED, null);

        ReservationWithItems marker = reservationMapper.selectReservationWithItems(1002);

        assertThat(marker.getOrderNo()).isEqualTo(1002);
        assertThat(marker.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(marker.getItems()).isEmpty();
    }

    @Test
    @DisplayName("예약 기록이 없는 주문 번호를 읽으면 결과가 없다")
    void unknownOrderNumberReturnsNothing() {
        assertThat(reservationMapper.selectReservationWithItems(9999)).isNull();
        assertThat(reservationMapper.selectStatusForUpdate(9999)).isNull();
    }

    @Test
    @DisplayName("예약 기록의 상태를 바꾸고, 잠그면서 읽는다")
    void updatesStatusAndReadsWithLock() {
        reservationMapper.insertReservation(1001, ReservationStatus.RESERVED, null);

        assertThat(reservationMapper.selectStatusForUpdate(1001)).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservationMapper.updateReservationStatus(1001, ReservationStatus.RELEASED))
                .isEqualTo(1);
        assertThat(reservationMapper.selectStatusForUpdate(1001)).isEqualTo(ReservationStatus.RELEASED);
    }
}
