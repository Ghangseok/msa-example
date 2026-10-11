package com.example.msa.order.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 재고 서비스 자리(WireMock)와 그 도우미다. 재고 서비스를 부르는 서비스 통합 테스트는 이 클래스를 상속한다.
 * 앱, MockMvc, 고객 흉내, DB 정리 같은 공통 준비는 {@link IntegrationTestSupport}에 있다.
 *
 * <p>재고 서비스 자리에는 JUnit 확장 {@link WireMockExtension}이 띄운 WireMock을 둔다. 재고 주소 설정
 * ({@code inventory-client.base-url})을 그 주소로 바꾼다. 주소는 JVM마다 한 번 정한 고정 포트라서 Spring 컨텍스트를
 * 테스트 클래스끼리 다시 쓸 수 있다. 예약과 해제의 스텁을 만들고, WireMock이 받은 요청을 읽는 것을 돕는다.
 */
public abstract class InventoryWireMock extends IntegrationTestSupport {

    private static final String RESERVATION_PATH = "/reservations/\\d+";
    private static final int WIREMOCK_PORT = findFreePort();

    /** 재고 서비스 자리다. 테스트 클래스 하나가 도는 동안 떠 있다. */
    @RegisterExtension
    protected static final WireMockExtension inventory = WireMockExtension.newInstance()
            .options(wireMockConfig().port(WIREMOCK_PORT).globalTemplating(true))
            .build();

    @DynamicPropertySource
    static void inventoryAddress(DynamicPropertyRegistry registry) {
        registry.add("inventory-client.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
    }

    /** 앞 테스트가 남긴 스텁과 받은 요청 기록을 지운다. */
    @BeforeEach
    void resetInventory() {
        inventory.resetAll();
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("WireMock이 쓸 빈 포트를 찾지 못했다", e);
        }
    }

    // ---- 재고 서비스 자리의 스텁 ----

    protected void stubReserve(ResponseDefinitionBuilder response) {
        inventory.stubFor(put(urlPathMatching(RESERVATION_PATH)).willReturn(response));
    }

    protected void stubRelease(ResponseDefinitionBuilder response) {
        inventory.stubFor(delete(urlPathMatching(RESERVATION_PATH)).willReturn(response));
    }

    /** 예약 결과 RESERVED. 주문 번호는 요청 경로에서 가져온다(WireMock 응답 템플릿). */
    protected static ResponseDefinitionBuilder reserved() {
        return reservationResult("RESERVED", null, List.of(), List.of());
    }

    /** 예약 결과 REJECTED. */
    protected static ResponseDefinitionBuilder rejected(String reason, List<String> shortage, List<String> missing) {
        return reservationResult("REJECTED", reason, shortage, missing);
    }

    private static ResponseDefinitionBuilder reservationResult(
            String status, String reason, List<String> shortage, List<String> missing) {
        String body = "{\"orderNo\":{{request.pathSegments.[1]}},\"status\":\"" + status + "\""
                + (reason == null ? "" : ",\"reason\":\"" + reason + "\"")
                + ",\"shortageProductIds\":" + jsonArray(shortage)
                + ",\"missingProductIds\":" + jsonArray(missing) + "}";
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    /** 해제 결과. {@code status}는 RELEASED 또는 REJECTED다. */
    protected static ResponseDefinitionBuilder releaseResult(String status) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"orderNo\":{{request.pathSegments.[1]}},\"status\":\"" + status + "\"}");
    }

    /** 재고 API 계약의 Problem Details 응답(409 RESERVATION_CONFLICT, 500 INTERNAL_ERROR 같은 것). */
    protected static ResponseDefinitionBuilder problem(int status, String code, String title) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"type\":\"urn:msa-example:problem:" + code + "\",\"title\":\"" + title + "\",\"status\":"
                        + status + ",\"code\":\"" + code + "\"}");
    }

    /** 계약에 없는 상태 코드(502, 503, 504)의 응답. 재고 앱이 아니라 네트워크와 인프라의 오류를 흉내 낸 것이다. */
    protected static ResponseDefinitionBuilder status(int status) {
        return aResponse().withStatus(status);
    }

    private static String jsonArray(List<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    // ---- 재고 서비스 자리가 받은 요청 ----

    /** 받은 예약 요청(PUT /reservations/{orderNo}). */
    protected List<LoggedRequest> reserveRequests() {
        return inventory.findAll(putRequestedFor(urlPathMatching(RESERVATION_PATH)));
    }

    /** 받은 예약 요청 본문의 항목들. 본문에 적힌 순서 그대로다. */
    protected static List<Item> itemsOf(LoggedRequest request) {
        List<Map<String, Object>> items = JsonPath.read(request.getBodyAsString(), "$.items");
        return items.stream()
                .map(item -> new Item((String) item.get("productId"), ((Number) item.get("quantity")).intValue()))
                .toList();
    }

    /** 받은 해제 요청(DELETE /reservations/{orderNo}). */
    protected List<LoggedRequest> releaseRequests() {
        return inventory.findAll(deleteRequestedFor(urlPathMatching(RESERVATION_PATH)));
    }

    /** 받은 요청과 돌려준 응답의 기록. 계약 검증에 쓴다. */
    protected List<ServeEvent> serveEvents() {
        return inventory.getAllServeEvents();
    }
}
