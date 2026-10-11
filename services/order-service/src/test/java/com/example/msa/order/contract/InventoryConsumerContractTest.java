package com.example.msa.order.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.msa.order.integration.InventoryWireMock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 소비자 계약 테스트: 주문 서비스가 쓰는 재고 API가 저장소 루트 contracts/inventory-api.yaml을 지키는지 본다.
 * 주문을 보내고, 주문 서비스가 재고 서비스 자리(WireMock)에 보낸 요청과 스텁이 돌려준 응답을
 * {@link ConsumerContractValidator}로 계약에 맞춰 검증한다.
 * 계약 테스트는 인수 시나리오 테스트를 받치는 테스트라서 테스트 케이스 번호를 붙이지 않는다.
 */
class InventoryConsumerContractTest extends InventoryWireMock {

    /** 서킷의 열림 시간(테스트 설정 2초)이 지나기를 기다리는 시간이다. 2초보다 조금 길게 둔다. */
    private static final Duration OPEN_DURATION_PASSED = Duration.ofMillis(2300);

    private final ConsumerContractValidator contract = new ConsumerContractValidator();

    @Test
    @DisplayName("RESERVED 스텁 응답과 받은 예약 요청이 계약을 지킨다 (시나리오 2.1의 주문)")
    void reservedStubAndReserveRequestFollowContract() {
        stubReserve(reserved());

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("B", 2));

        assertThat(reply.status()).isEqualTo(201);
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("REJECTED(OUT_OF_STOCK) 스텁 응답과 받은 예약 요청이 계약을 지킨다 (시나리오 3.1의 주문)")
    void outOfStockStubAndReserveRequestFollowContract() {
        stubReserve(rejected("OUT_OF_STOCK", List.of("B"), List.of()));

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("B", 2));

        // 스텁 응답이 주문 결과로 이어졌는지도 본다. 계약만 검증하면 거절 처리가 없어도 통과한다.
        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("409 스텁 응답과 해제 스텁 응답, 받은 예약·해제 요청이 계약을 지킨다 (시나리오 7.2의 주문)")
    void conflictStubAndReleaseStubFollowContract() {
        stubReserve(problem(409, "RESERVATION_CONFLICT", "같은 주문 번호에 다른 내용의 예약 요청이 왔다"));
        stubRelease(releaseResult("RELEASED"));

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        assertThat(reply.status()).isEqualTo(201);
        // 해제는 고객 응답 뒤에 비동기로 가므로, 해제 요청이 올 때까지 기다린다.
        await().atMost(Duration.ofSeconds(10)).until(() -> releaseRequests().size() == 1);
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("503 스텁은 받은 요청만 검증하고, 회복 뒤 RESERVED 스텁은 계약을 지킨다 (시나리오 7.1의 주문)")
    void recoveryStubsFollowContract() throws InterruptedException {
        // 503은 계약에 없는 응답이라 응답은 검증에서 빠지고, 받은 예약·해제 요청만 검증한다.
        stubReserve(status(503));
        stubRelease(status(503));
        Reply failed = placeOrder(C1, newKey(), new Item("A", 1));
        assertThat(failed.status()).isEqualTo(201);
        assertThat(failed.text("$.status")).isEqualTo("FAILED");
        // 해제는 고객 응답 뒤에 비동기로 가고, 해제 요청이 503이면 재시도한다. 요청 수가 아니라 첫 주문의 해제가 끝나기를 기다린다.
        await().atMost(Duration.ofSeconds(10))
                .until(() -> orderRow(failed.orderNo()).get("RELEASE_STATUS") != null);
        // 첫 주문의 예약 6번과 해제 4번이 모두 실패해 서킷이 열렸다. 열림 시간(테스트 설정 2초)이 지나기를 기다린다.
        Thread.sleep(OPEN_DURATION_PASSED.toMillis());

        // 재고 서비스가 정상으로 돌아온다.
        stubReserve(reserved());
        stubRelease(releaseResult("RELEASED"));
        Reply recovered = placeOrder(C1, newKey(), new Item("A", 1));

        assertThat(recovered.text("$.status")).isEqualTo("CONFIRMED");
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("503 두 번 뒤의 RESERVED 스텁 응답과 받은 예약 요청 세 개가 계약을 지킨다 (시나리오 8.1의 주문)")
    void retriedReserveRequestsAndReservedStubFollowContract() {
        // 503은 계약에 없는 응답이라 응답은 검증에서 빠진다. 받은 예약 요청 세 개와 RESERVED 스텁 응답을 검증한다.
        stubReserveSequence(status(503), status(503), reserved());

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        assertThat(reply.text("$.status")).isEqualTo("CONFIRMED");
        assertThat(reserveRequests()).hasSize(3);
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("지연된 RESERVED 스텁 응답, 해제 스텁 응답, 받은 예약·해제 요청이 계약을 지킨다 (시나리오 9.1의 주문)")
    void delayedReservedStubAndReleaseFollowContract() {
        // 예약 응답이 시도당 제한 시간(테스트 설정 500ms)보다 늦게(600ms) 와서 여섯 시도가 모두 시간 초과로 끝난다.
        stubReserve(reserved().withFixedDelay(600));
        stubRelease(releaseResult("RELEASED"));

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3));

        assertThat(reply.text("$.status")).isEqualTo("FAILED");
        assertThat(reserveRequests()).hasSize(6);
        // 해제는 고객 응답 뒤에 비동기로 가므로, 해제 요청이 올 때까지 기다린다.
        await().atMost(Duration.ofSeconds(10)).until(() -> releaseRequests().size() == 1);
        // 시간 초과로 끊긴 예약 요청의 응답 기록이 모두 남을 때까지 기다린다. 지연이 끝나야 WireMock이 응답을 기록한다.
        await().atMost(Duration.ofSeconds(10))
                .until(() -> serveEvents().stream()
                        .allMatch(event -> event.getResponse() != null
                                && event.getResponse().getStatus() > 0));
        contract.assertAllConform(serveEvents());
    }

    @Test
    @DisplayName("REJECTED(PRODUCT_NOT_FOUND) 스텁 응답과 받은 예약 요청이 계약을 지킨다 (시나리오 3.3의 주문)")
    void productNotFoundStubAndReserveRequestFollowContract() {
        stubReserve(rejected("PRODUCT_NOT_FOUND", List.of(), List.of("Z")));

        Reply reply = placeOrder(C1, newKey(), new Item("A", 3), new Item("Z", 1));

        assertThat(reply.text("$.status")).isEqualTo("REJECTED");
        contract.assertAllConform(serveEvents());
    }
}
