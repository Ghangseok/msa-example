package com.example.msa.order.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.example.msa.order.OracleTestContainer;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * 주문 서비스의 서비스 통합 테스트가 함께 쓰는 준비 코드다. 재고 서비스의 같은 이름의 클래스와 공유하지 않고 주문 서비스에 따로 둔다.
 * 재고 서비스 자리(WireMock)가 필요한 테스트는 이 클래스를 직접 상속하지 않고 {@link InventoryWireMock}을 상속한다.
 *
 * <ul>
 *   <li>앱 전체를 {@code @SpringBootTest}와 MockMvc로 부르고, DB는 Testcontainers의 실제 Oracle이다.
 *   <li>고객은 Spring Security Test의 {@code jwt()}로 흉내 낸다. Keycloak은 필요 없다.
 *   <li>테스트는 자기가 쓸 데이터를 직접 만들고, 시작할 때와 끝날 때 주문 테이블을 비운다. 재고 서킷은 매 테스트 시작에 닫는다.
 * </ul>
 *
 * <p>이 클래스는 기대값을 정하지 않는다. 기대값은 각 테스트가 {@code docs/test-cases/order-placement.md}에서 그대로 가져온다.
 * 아직 없는 구현 클래스를 참조하지 않는다. 응답은 JSON 글자로 읽는다. 그래야 구현 전의 테스트가 컴파일 오류가 아니라 단언 실패로 끝난다.
 */
@SpringBootTest
@ActiveProfiles("test")
@ImportTestcontainers(OracleTestContainer.class)
public abstract class IntegrationTestSupport {

    /** 고객 C1, C2의 JWT {@code sub} 값이다. 테스트 안에서 정한 문자열이다. */
    protected static final String C1 = "customer-c1";

    protected static final String C2 = "customer-c2";

    private static final Logger log = LoggerFactory.getLogger(IntegrationTestSupport.class);

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private WebApplicationContext context;

    /** 서킷 브레이커 빈이 생기기 전의 테스트에서도 컨텍스트가 뜨도록 ObjectProvider로 받는다. */
    @Autowired
    private ObjectProvider<CircuitBreaker> circuitBreaker;

    protected MockMvc mvc;

    @BeforeEach
    void prepare() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        circuitBreaker.ifAvailable(CircuitBreaker::reset);
        deleteOrders();
    }

    @AfterEach
    void deleteOrders() {
        jdbc.update("DELETE FROM ORDER_ITEMS");
        jdbc.update("DELETE FROM ORDERS");
    }

    // ---- 주문 요청 ----

    /** 주문 항목 하나. */
    public record Item(String productId, int quantity) {}

    /** 응답의 기록. 오류 상태도 예외 없이 담는다. */
    public record Reply(int status, String contentType, String body) {

        /** 응답 본문에서 JsonPath로 글자 값을 읽는다. */
        public String text(String jsonPath) {
            Object value = JsonPath.read(body, jsonPath);
            return value == null ? null : value.toString();
        }

        /** 응답 본문에서 JsonPath로 글자 목록을 읽는다. */
        public List<String> texts(String jsonPath) {
            List<?> values = JsonPath.read(body, jsonPath);
            return values.stream().map(String::valueOf).collect(Collectors.toList());
        }

        /** 응답 본문의 주문 번호다. */
        public long orderNo() {
            return ((Number) JsonPath.read(body, "$.orderNo")).longValue();
        }
    }

    /** 요청 본문 JSON. 항목 순서는 호출한 순서 그대로다. */
    protected static String orderJson(Item... items) {
        return "{\"items\":["
                + Arrays.stream(items)
                        .map(item ->
                                "{\"productId\":\"" + item.productId() + "\",\"quantity\":" + item.quantity() + "}")
                        .collect(Collectors.joining(","))
                + "]}";
    }

    /** 주문 요청 키(UUID 문자열)를 새로 만든다. */
    protected static String newKey() {
        return UUID.randomUUID().toString();
    }

    /** 고객을 JWT로 흉내 낸다. {@code sub}가 고객 ID다. */
    protected static RequestPostProcessor customer(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    /** POST /orders. {@code auth}가 null이면 토큰 없이, {@code key}가 null이면 요청 키 헤더 없이 보낸다. */
    protected Reply postOrder(RequestPostProcessor auth, String key, String body) {
        MockHttpServletRequestBuilder request =
                post("/orders").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        if (auth != null) {
            request.with(auth);
        }
        try {
            MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
            return new Reply(
                    response.getStatus(),
                    response.getContentType(),
                    response.getContentAsString(StandardCharsets.UTF_8));
        } catch (ServletException e) {
            // 처리되지 않은 예외다. MockMvc는 예외를 밖으로 던지지만, 실제 서버에서는 500 응답이 된다.
            log.warn("POST /orders가 처리되지 않은 예외로 끝났다. 500으로 본다", e);
            return new Reply(500, null, "");
        } catch (Exception e) {
            throw new IllegalStateException("POST /orders 요청을 보내지 못했다", e);
        }
    }

    /** 고객 {@code sub}가 요청 키 {@code key}로 주문한다. */
    protected Reply placeOrder(String sub, String key, Item... items) {
        return postOrder(customer(sub), key, orderJson(items));
    }

    // ---- 저장된 주문 ----

    protected int orderCount() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ORDERS", Integer.class);
        return count == null ? -1 : count;
    }

    /** 주문 한 줄. 키는 열 이름(CUSTOMER_ID, STATUS, REASON, RELEASE_STATUS)이다. 주문이 없으면 null이다. */
    protected Map<String, Object> orderRow(long orderNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT CUSTOMER_ID, REQUEST_KEY, STATUS, REASON, RELEASE_STATUS FROM ORDERS WHERE ORDER_NO = ?",
                orderNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 주문 항목들. 상품 ID 순이고, 키는 열 이름(PRODUCT_ID, QUANTITY, REJECT_REASON)이다. */
    protected List<Map<String, Object>> itemRows(long orderNo) {
        return jdbc.queryForList(
                "SELECT PRODUCT_ID, QUANTITY, REJECT_REASON FROM ORDER_ITEMS WHERE ORDER_NO = ? ORDER BY PRODUCT_ID",
                orderNo);
    }
}
