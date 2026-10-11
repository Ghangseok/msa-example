package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 주문의 주인이 토큰의 고객인지 본다. 인수 시나리오 테스트가 아니라서 테스트 케이스 번호를 붙이지 않는다.
 *
 * <p>기대값은 유스케이스 6절 업무 규칙 "주문은 로그인한 고객 본인의 이름으로만 만든다"
 * ({@code docs/requirements/uc-001-place-order.md})다. C1의 토큰으로 주문하면서, 요청 본문에 계약에 없는 필드
 * {@code customerId}와 헤더 {@code X-Customer-Id}를 넣고 두 값을 C2의 {@code sub}로 채운다 [제안].
 * 저장된 주문의 고객은 C1이어야 한다.
 */
class CustomerIdentityIntegrationTest extends InventoryWireMock {

    @Test
    @DisplayName("요청 본문의 customerId와 X-Customer-Id 헤더를 믿지 않고 토큰의 고객으로 주문을 만든다")
    void orderOwnerIsTheTokenCustomer() throws Exception {
        stubReserve(reserved());
        String body = "{\"customerId\":\"" + C2 + "\",\"items\":[{\"productId\":\"A\",\"quantity\":3}]}";

        MockHttpServletResponse response = mvc.perform(post("/orders")
                        .with(customer(C1))
                        .header("Idempotency-Key", newKey())
                        .header("X-Customer-Id", C2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(201);
        Reply reply = new Reply(
                response.getStatus(), response.getContentType(), response.getContentAsString(StandardCharsets.UTF_8));
        Map<String, Object> order = orderRow(reply.orderNo());
        assertThat(order.get("CUSTOMER_ID")).isEqualTo(C1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ORDERS WHERE CUSTOMER_ID = ?", Integer.class, C2))
                .isZero();
    }
}
