package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시나리오 6.1의 주문 쪽: 같은 주문 요청 키로 다시 보내면 주문은 하나다. 테스트 케이스의 When 일곱 개를 모두 검증한다.
 * 재고 수량 기대값(7개, 4개)은 재고 서비스 테스트가 보고, 주문 쪽에서는 WireMock이 받은 예약 요청의 횟수와 주문 번호로 본다.
 * K1, K2, K3은 이 테스트에서 만든 UUID다. 응답 코드는 테스트 케이스 문서의 공통 전제를 따른다.
 * 새 주문은 201, 같은 고객이 같은 요청 키로 다시 보낸 요청은 200이다.
 */
class IdempotencyKeyIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("TC-011 같은 주문 요청 키로 다시 보내면 주문은 하나다")
    void sameRequestKeyYieldsOneOrder() {
        // Given 상품 A의 재고가 10개다. 재고 서비스 자리가 RESERVED를 돌려준다.
        stubReserve(reserved());
        String k1 = newKey();

        // When 고객이 요청 키 K1로 상품 A 3개를 주문하고, 같은 키 K1로 같은 요청을 한 번 더 보낸다
        Reply first = placeOrder(C1, k1, new Item("A", 3));
        Reply again = placeOrder(C1, k1, new Item("A", 3));

        // Then 두 응답의 주문 번호가 같다
        assertThat(first.status()).isEqualTo(201);
        assertThat(again.status()).isEqualTo(200);
        assertThat(again.orderNo()).isEqualTo(first.orderNo());
        // And 상품 A의 재고는 7개다: 재고 서비스에는 예약 요청이 한 번만 갔다
        assertThat(reserveRequests()).hasSize(1);
        assertThat(orderCount()).isEqualTo(1);

        // When 고객이 새 요청 키 K2로 같은 내용을 주문한다
        Reply second = placeOrder(C1, newKey(), new Item("A", 3));

        // Then 새 주문 번호를 받고 상품 A의 재고는 4개다: 예약 요청이 새 주문 번호로 한 번 더 갔다
        assertThat(second.status()).isEqualTo(201);
        assertThat(second.orderNo()).isNotEqualTo(first.orderNo());
        assertThat(requestedOrderNumbers()).containsExactlyInAnyOrder(first.orderNo(), second.orderNo());

        // When 고객이 K1으로 상품 A 5개를 주문한다
        Reply conflict = placeOrder(C1, k1, new Item("A", 5));

        // Then 422(요청 키 충돌)를 받는다. 새 주문도 새 예약 요청도 없다.
        assertThat(conflict.status()).isEqualTo(422);
        assertThat(orderCount()).isEqualTo(2);
        assertThat(reserveRequests()).hasSize(2);

        // When C2가 K1으로 상품 A 1개를 주문한다
        Reply otherCustomer = placeOrder(C2, k1, new Item("A", 1));

        // Then 새 주문이 만들어진다 (키는 고객마다 따로 본다)
        assertThat(otherCustomer.status()).isEqualTo(201);
        assertThat(otherCustomer.orderNo()).isNotIn(first.orderNo(), second.orderNo());
        assertThat(orderRow(otherCustomer.orderNo()).get("CUSTOMER_ID")).isEqualTo(C2);
        assertThat(orderCount()).isEqualTo(3);

        // When 요청 키 없이 주문한다
        Reply withoutKey = postOrder(customer(C1), null, orderJson(new Item("A", 1)));

        // Then 400을 받고 주문은 기록되지 않는다
        assertThat(withoutKey.status()).isEqualTo(400);
        assertThat(orderCount()).isEqualTo(3);

        // When UUID 형식이 아닌 요청 키로 주문한다
        Reply malformedKey = postOrder(customer(C1), "not-a-uuid", orderJson(new Item("A", 1)));

        // Then 400을 받고 주문은 기록되지 않는다
        assertThat(malformedKey.status()).isEqualTo(400);
        assertThat(orderCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-011 첫 요청이 처리 중일 때 같은 키로 다시 보내면 같은 주문 번호와 처리중을 받는다")
    void sameRequestKeyWhileProcessingReturnsPending() throws InterruptedException, ExecutionException {
        // Given 재고 서비스의 응답이 늦다. 지연은 시도당 제한 시간(테스트 설정 500ms)보다 짧게 둔다.
        stubReserve(reserved().withFixedDelay(300));
        String k3 = newKey();

        // When 고객이 요청 키 K3로 상품 A 1개를 주문하고, 그 요청이 아직 처리 중일 때 같은 키 K3로 같은 요청을 한 번 더 보낸다
        CompletableFuture<Reply> firstRequest =
                CompletableFuture.supplyAsync(() -> placeOrder(C1, k3, new Item("A", 1)));
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(5)).until(() -> orderCount() == 1);
        Reply secondRequest = placeOrder(C1, k3, new Item("A", 1));

        // Then 둘째 응답은 첫 요청과 같은 주문 번호와 "처리중"을 받는다
        Reply first = firstRequest.get();
        assertThat(secondRequest.status()).isEqualTo(200);
        assertThat(secondRequest.text("$.status")).isEqualTo("PENDING");
        assertThat(secondRequest.orderNo()).isEqualTo(first.orderNo());
        // And 주문은 하나만 기록된다
        assertThat(orderCount()).isEqualTo(1);
    }

    /** WireMock이 받은 예약 요청의 주문 번호들이다. 경로 /reservations/{orderNo}의 끝이다. */
    private List<Long> requestedOrderNumbers() {
        return reserveRequests().stream()
                .map(LoggedRequest::getUrl)
                .map(url -> Long.valueOf(url.substring(url.lastIndexOf('/') + 1)))
                .toList();
    }
}
