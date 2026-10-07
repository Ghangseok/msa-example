package com.example.msa.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.inventory.dto.ReserveRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 새 예약의 판단(RESERVED 또는 REJECTED)과 거절 사유의 우선순위를 본다. Spring 없이 돈다.
 *
 * <p>기대값의 출처는 설계 문서 6절 "예약 처리" 2번과 3번이다. "행이 없는 상품은 없는 상품 목록에, 수량이 모자란 상품은 부족한 상품 목록에 모은다",
 * "없는 상품이 하나라도 있으면 상품 없음, 아니면 재고 부족이고 두 목록은 모두 돌려준다"이다.
 * 목록이 상품 ID 순인 것은 contracts/inventory-api.yaml이 정한다. 값은 시나리오 2.1, 3.1, 3.3(변형)의 값을 쓴다.
 */
class ReservationDecisionReasonTest {

    private static ReserveRequest.Item item(String productId, int quantity) {
        return new ReserveRequest.Item(productId, quantity);
    }

    @Test
    @DisplayName("없는 상품(Z)과 부족한 상품(B)이 함께 있으면 사유는 상품 없음이고 두 목록이 모두 찬다")
    void missingProductWinsOverShortage() {
        // 시나리오 3.3 변형: 상품 A의 재고가 10개, 상품 B의 재고가 1개이고, 상품 Z는 재고 데이터에 없다
        Map<String, Integer> stock = Map.of("A", 10, "B", 1);

        ReservationDecision decision =
                ReservationDecision.decide(List.of(item("A", 3), item("Z", 1), item("B", 2)), stock);

        assertThat(decision.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(decision.reason()).isEqualTo(ReservationReason.PRODUCT_NOT_FOUND);
        assertThat(decision.missingProductIds()).containsExactly("Z");
        assertThat(decision.shortageProductIds()).containsExactly("B");
        // 항목마다 결과를 남긴다. 거절의 원인이 아닌 상품은 비어 있다.
        assertThat(decision.resultOf("Z")).isEqualTo(ReservationReason.PRODUCT_NOT_FOUND);
        assertThat(decision.resultOf("B")).isEqualTo(ReservationReason.OUT_OF_STOCK);
        assertThat(decision.resultOf("A")).isNull();
    }

    @Test
    @DisplayName("없는 상품이 없고 부족한 상품만 있으면 사유는 재고 부족이다")
    void onlyShortageGivesOutOfStock() {
        // 시나리오 3.1: 상품 A의 재고가 10개, 상품 B의 재고가 1개다. 요청은 A 3개, B 2개
        Map<String, Integer> stock = Map.of("A", 10, "B", 1);

        ReservationDecision decision = ReservationDecision.decide(List.of(item("A", 3), item("B", 2)), stock);

        assertThat(decision.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(decision.reason()).isEqualTo(ReservationReason.OUT_OF_STOCK);
        assertThat(decision.shortageProductIds()).containsExactly("B");
        assertThat(decision.missingProductIds()).isEmpty();
    }

    @Test
    @DisplayName("모든 항목의 재고가 충분하면 예약되고 사유와 목록이 비어 있다")
    void sufficientStockIsReserved() {
        // 시나리오 2.1: 상품 A의 재고가 10개, 상품 B의 재고가 5개다. 요청은 A 3개, B 2개
        Map<String, Integer> stock = Map.of("A", 10, "B", 5);

        ReservationDecision decision = ReservationDecision.decide(List.of(item("A", 3), item("B", 2)), stock);

        assertThat(decision.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(decision.reason()).isNull();
        assertThat(decision.shortageProductIds()).isEmpty();
        assertThat(decision.missingProductIds()).isEmpty();
    }

    @Test
    @DisplayName("부족한 상품 목록과 없는 상품 목록은 상품 ID 순이다")
    void listsAreSortedByProductId() {
        Map<String, Integer> stock = Map.of("A", 0, "B", 0);

        ReservationDecision decision =
                ReservationDecision.decide(List.of(item("Z", 1), item("B", 1), item("Y", 1), item("A", 1)), stock);

        assertThat(decision.shortageProductIds()).containsExactly("A", "B");
        assertThat(decision.missingProductIds()).containsExactly("Y", "Z");
    }
}
