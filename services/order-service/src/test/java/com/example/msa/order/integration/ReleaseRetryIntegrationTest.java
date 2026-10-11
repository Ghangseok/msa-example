package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 시나리오 9.2: 해제 요청도 일시 오류면 같은 정책으로 재시도한다.
 *
 * <p>해제는 고객 응답 뒤에 따로 도므로, 해제가 끝나기를 저장된 주문을 다시 읽으며 기다린다 [제안]. 기다리는 한도는 테스트 케이스에
 * 값이 없어 단언하지 않고, 테스트가 끝없이 돌지 않게 하는 안전장치로만 둔다(10초) [제안].
 *
 * <p>시간은 시간 값을 1/5로 줄인 테스트 설정을 따른다 [제안]. 테스트 케이스의 "첫 번째 응답을 받은 뒤 0.5초 안에"는 0.1초 안에,
 * "0.8초 이상 1.5초 이하"는 0.16초 이상 0.3초 이하로 줄였다. 응답을 받은 시각은 WireMock이 다음 요청을 받은 시각으로 대신 잰다 [제안].
 */
class ReleaseRetryIntegrationTest extends InventoryWireMock {

    private static final Logger log = LoggerFactory.getLogger(ReleaseRetryIntegrationTest.class);

    /** 기다림의 안전장치다. 단언하는 값이 아니다. */
    private static final Duration SAFETY_LIMIT = Duration.ofSeconds(10);

    /** "첫 번째 응답을 받은 뒤 0.5초 안에"를 1/5로 줄인 값이다. */
    private static final Duration IMMEDIATE_RETRY_LIMIT = Duration.ofMillis(100);

    /** "두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하"를 1/5로 줄인 값이다. */
    private static final Duration SECOND_RETRY_MIN = Duration.ofMillis(160);

    private static final Duration SECOND_RETRY_MAX = Duration.ofMillis(300);

    @Test
    @DisplayName("TC-019 해제 요청도 일시 오류면 같은 정책으로 재시도한다")
    void releaseIsRetriedWithTheSamePolicy() {
        // Given 재고 서비스가 예약 요청에 409를 돌려준다
        stubReserve(problem(409, "RESERVATION_CONFLICT", "같은 주문 번호에 다른 내용의 예약 요청이 왔다"));
        // And 재고 서비스가 해제 요청의 처음 두 번에 503을 돌려주고, 세 번째부터 정상으로 처리한다
        stubReleaseSequence(status(503), status(503), releaseResult("RELEASED"));

        // When 고객이 상품 A를 3개 주문한다
        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        // Then 고객은 주문 번호와 "잠시 후 다시 시도"를 받는다
        assertThat(reply.status()).isEqualTo(201);
        long orderNo = reply.orderNo();
        assertThat(reply.text("$.status")).isEqualTo("FAILED");
        assertThat(reply.text("$.reason")).isEqualTo("RETRY_LATER");

        // And 주문에 "해제 완료"가 기록된다
        await().atMost(SAFETY_LIMIT)
                .until(() -> "RELEASED".equals(orderRow(orderNo).get("RELEASE_STATUS")));

        // And 해제 요청은 정확히 3번 갔다
        List<LoggedRequest> releases = inReceivedOrder(releaseRequests());
        assertThat(releases).hasSize(3);
        assertThat(releases).allSatisfy(request -> assertThat(request.getUrl()).isEqualTo("/reservations/" + orderNo));

        // And 두 번째는 첫 번째 응답을 받은 뒤 0.5초 안에, 세 번째는 두 번째 응답을 받은 뒤 0.8초 이상 1.5초 이하에 갔다 (1/5로 줄인 값)
        Duration firstToSecond = gap(releases.get(0), releases.get(1));
        Duration secondToThird = gap(releases.get(1), releases.get(2));
        log.info("해제 재시도 간격: 첫째-둘째 {} ms, 둘째-셋째 {} ms", firstToSecond.toMillis(), secondToThird.toMillis());
        assertThat(firstToSecond).isLessThan(IMMEDIATE_RETRY_LIMIT);
        assertThat(secondToThird).isBetween(SECOND_RETRY_MIN, SECOND_RETRY_MAX);
    }
}
