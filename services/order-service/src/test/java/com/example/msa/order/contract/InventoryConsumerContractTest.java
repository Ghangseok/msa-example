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
    void recoveryStubsFollowContract() {
        // 503은 계약에 없는 응답이라 응답은 검증에서 빠지고, 받은 예약·해제 요청만 검증한다.
        stubReserve(status(503));
        stubRelease(status(503));
        Reply failed = placeOrder(C1, newKey(), new Item("A", 1));
        assertThat(failed.status()).isEqualTo(201);
        assertThat(failed.text("$.status")).isEqualTo("FAILED");
        await().atMost(Duration.ofSeconds(10)).until(() -> releaseRequests().size() == 1);

        // 재고 서비스가 정상으로 돌아온다.
        stubReserve(reserved());
        stubRelease(releaseResult("RELEASED"));
        Reply recovered = placeOrder(C1, newKey(), new Item("A", 1));

        assertThat(recovered.text("$.status")).isEqualTo("CONFIRMED");
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
