package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 시나리오 8.1: 재고 서비스의 일시 오류는 재시도로 넘어간다.
 *
 * <p>재고 서비스 쪽 기대값("상품 A의 재고는 7개다")은 재고 서비스 테스트(RetriedReservationStockIntegrationTest)가 본다.
 * 이 테스트는 주문 쪽을 본다. 같은 주문 번호와 같은 본문으로 정확히 3번 보냈는지, 그 간격이 맞는지 본다.
 *
 * <p>시간은 시간 값을 1/5로 줄인 테스트 설정을 따른다 [제안]. 테스트 케이스의 "첫 번째 응답을 받은 뒤 0.5초 안에"는 0.1초 안에,
 * "0.8초 이상 1.5초 이하"는 0.16초 이상 0.3초 이하로 줄였다. 응답을 받은 시각은 WireMock이 다음 요청을 받은 시각으로 대신 잰다
 * [제안]. 503 스텁에는 지연이 없어서 응답을 받은 시각과 요청을 받은 시각의 차이가 작기 때문이다.
 */
class TransientErrorRetryIntegrationTest extends InventoryWireMock {

    private static final Logger log = LoggerFactory.getLogger(TransientErrorRetryIntegrationTest.class);

    /** "첫 번째 응답을 받은 뒤 0.5초 안에"를 1/5로 줄인 값이다. */
    private static final Duration IMMEDIATE_RETRY_LIMIT = Duration.ofMillis(100);

    /** "두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하"를 1/5로 줄인 값이다. */
    private static final Duration SECOND_RETRY_MIN = Duration.ofMillis(160);

    private static final Duration SECOND_RETRY_MAX = Duration.ofMillis(300);

    @Test
    @DisplayName("TC-006 재고 서비스의 일시 오류는 재시도로 넘어간다")
    void transientErrorsAreRetriedWithTheSameOrderNo() {
        // Given 재고 서비스가 처음 두 번의 예약 요청에 503을 돌려주고, 세 번째부터 정상으로 처리한다
        stubReserveSequence(status(503), status(503), reserved());

        // When 고객이 상품 A를 3개 주문한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        // Then 고객은 "확정"을 받는다
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.text("$.status")).isEqualTo("CONFIRMED");
        long orderNo = reply.orderNo();
        assertThat(orderRow(orderNo).get("STATUS")).isEqualTo("CONFIRMED");

        // And 예약 요청은 정확히 3번 갔다. 세 요청의 주문 번호와 본문이 같다
        List<LoggedRequest> requests = inReceivedOrder(reserveRequests());
        assertThat(requests).hasSize(3);
        for (LoggedRequest request : requests) {
            assertThat(request.getUrl()).isEqualTo("/reservations/" + orderNo);
            assertThat(itemsOf(request)).containsExactly(new Item("A", 3));
            assertThat(request.getBodyAsString()).isEqualTo(requests.get(0).getBodyAsString());
        }

        // And 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다 (1/5로 줄인 값)
        Duration firstToSecond = gap(requests.get(0), requests.get(1));
        Duration secondToThird = gap(requests.get(1), requests.get(2));
        log.info("재시도 간격: 첫째-둘째 {} ms, 둘째-셋째 {} ms", firstToSecond.toMillis(), secondToThird.toMillis());
        assertThat(firstToSecond).isLessThan(IMMEDIATE_RETRY_LIMIT);
        assertThat(secondToThird).isBetween(SECOND_RETRY_MIN, SECOND_RETRY_MAX);
    }
}
