package com.example.msa.inventory.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.inventory.integration.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 제공자 계약 테스트: 재고 API가 저장소 루트 contracts/inventory-api.yaml을 지키는지 본다.
 * 실제 요청을 보내고 요청과 응답을 {@link ProviderContractValidator}로 계약에 맞춰 검증한다.
 * 계약 테스트는 인수 시나리오 테스트를 받치는 테스트라서 테스트 케이스 번호를 붙이지 않는다.
 */
class ReservationProviderContractTest extends IntegrationTestSupport {

    private final ProviderContractValidator contract = new ProviderContractValidator();

    @Test
    @DisplayName("예약 PUT 200 응답이 계약을 지킨다 (시나리오 1.1의 요청)")
    void reserveResponseFollowsContract() {
        insertStock("A", 10);

        Reply reply = reserve(1001, new Item("A", 3));

        assertThat(reply.status()).isEqualTo(200);
        assertConforms(reply);
    }

    @Test
    @DisplayName("해제 DELETE 200 응답이 계약을 지킨다 (시나리오 1.3의 요청)")
    void releaseResponseFollowsContract() {
        insertStock("A", 10);

        Reply reply = release(1002);

        assertThat(reply.status()).isEqualTo(200);
        assertConforms(reply);
    }

    @Test
    @DisplayName("409 Problem Details 응답이 계약을 지킨다 (시나리오 1.4의 요청)")
    void conflictResponseFollowsContract() {
        insertStock("A", 10);
        assertThat(reserve(1003, new Item("A", 3)).status()).isEqualTo(200);

        Reply reply = reserve(1003, new Item("A", 5));

        assertThat(reply.status()).isEqualTo(409);
        assertConforms(reply);
    }

    @Test
    @DisplayName("PRODUCT_NOT_FOUND 거절 응답이 계약을 지킨다 (시나리오 3.3의 재고 쪽 요청)")
    void productNotFoundResponseFollowsContract() {
        insertStock("A", 10);

        Reply reply = reserve(3, new Item("A", 3), new Item("Z", 1));

        assertThat(reply.status()).isEqualTo(200);
        assertConforms(reply);
    }

    @Test
    @DisplayName("거절된 예약의 해제 DELETE 200(REJECTED) 응답이 계약을 지킨다 (시나리오 1.6의 요청)")
    void releaseOfRejectedReservationFollowsContract() {
        insertStock("A", 2);
        assertThat(reserve(2001, new Item("A", 3)).status()).isEqualTo(200);

        Reply reply = release(2001);

        assertThat(reply.status()).isEqualTo(200);
        assertConforms(reply);
    }

    private void assertConforms(Reply reply) {
        contract.assertConforms(
                reply.method(), reply.path(), reply.requestBody(), reply.status(), reply.contentType(), reply.body());
    }
}
