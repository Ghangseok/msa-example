package com.example.msa.inventory.integration;

import com.example.msa.inventory.OracleTestContainer;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * 재고 서비스의 서비스 통합 테스트가 함께 쓰는 준비 코드다.
 * 앱을 임의 포트로 띄우고 실제 HTTP 요청을 보낸다. DB는 Testcontainers의 실제 Oracle이다.
 *
 * <p>이 클래스는 값을 정하지 않는다. 기대값은 각 테스트가 {@code docs/test-cases/order-placement.md}에서 그대로 가져온다.
 * 아직 없는 구현 클래스를 참조하지 않는다. 요청은 JSON 글자로 보내고 응답은 JsonPath로 읽는다.
 * 그래야 구현 전의 테스트가 컴파일 오류가 아니라 단언 실패로 끝난다.
 *
 * <p>테스트는 자기가 쓸 상품 행(A, B, Z)과 예약 기록을 직접 넣고, 끝나면 지운다.
 * Flyway 초기 데이터(P-001부터 P-005)는 건드리지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ImportTestcontainers(OracleTestContainer.class)
public abstract class IntegrationTestSupport {

    /** 응답을 기다리는 최대 시간. 구현에 문제가 있어도 테스트가 끝없이 돌지 않게 막는다. */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(60);

    @LocalServerPort
    private int port;

    @Autowired
    protected JdbcTemplate jdbc;

    private RestClient client;

    @BeforeEach
    void prepareClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT);
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .requestFactory(requestFactory)
                .build();
        deleteTestData();
    }

    @AfterEach
    void deleteTestData() {
        jdbc.update("DELETE FROM RESERVATION_ITEMS");
        jdbc.update("DELETE FROM RESERVATIONS");
        jdbc.update("DELETE FROM STOCK WHERE PRODUCT_ID IN ('A', 'B', 'Z')");
    }

    /** 예약 요청의 항목 하나. */
    public record Item(String productId, int quantity) {}

    /** 요청과 응답의 기록. 오류 상태도 예외 없이 담는다. */
    public record Reply(String method, String path, String requestBody, int status, String contentType, String body) {

        /**
         * 업무 결과, 곧 응답 본문의 status(RESERVED, REJECTED, RELEASED)다.
         * 상태 코드가 200이 아니면 "HTTP 404"처럼 돌려줘서, 단언이 실패할 때 메시지에 원인이 보인다.
         */
        public String result() {
            return status == 200 ? text("$.status") : "HTTP " + status;
        }

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
    }

    protected void insertStock(String productId, int quantity) {
        jdbc.update("INSERT INTO STOCK (PRODUCT_ID, QUANTITY) VALUES (?, ?)", productId, quantity);
    }

    protected void updateStock(String productId, int quantity) {
        jdbc.update("UPDATE STOCK SET QUANTITY = ? WHERE PRODUCT_ID = ?", quantity, productId);
    }

    protected int stockOf(String productId) {
        Integer quantity =
                jdbc.queryForObject("SELECT QUANTITY FROM STOCK WHERE PRODUCT_ID = ?", Integer.class, productId);
        return quantity == null ? -1 : quantity;
    }

    /** PUT /reservations/{orderNo}. 항목 순서는 호출한 순서 그대로 보낸다. */
    protected Reply reserve(long orderNo, Item... items) {
        String body = "{\"items\":["
                + Arrays.stream(items)
                        .map(item ->
                                "{\"productId\":\"" + item.productId() + "\",\"quantity\":" + item.quantity() + "}")
                        .collect(Collectors.joining(","))
                + "]}";
        return send("PUT", "/reservations/" + orderNo, body);
    }

    /** DELETE /reservations/{orderNo}. */
    protected Reply release(long orderNo) {
        return send("DELETE", "/reservations/" + orderNo, null);
    }

    private Reply send(String method, String path, String body) {
        RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(method))
                .uri(path)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON);
        if (body != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return spec.exchange((request, response) -> {
            String responseBody = response.bodyTo(String.class);
            MediaType contentType = response.getHeaders().getContentType();
            return new Reply(
                    method,
                    path,
                    body,
                    response.getStatusCode().value(),
                    contentType == null ? null : contentType.toString(),
                    responseBody);
        });
    }

    /** 작업을 모두 준비시킨 뒤 한꺼번에 출발시키고, 끝난 결과를 작업 순서대로 돌려준다. */
    protected <T> List<T> runConcurrently(List<Callable<T>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(RESPONSE_TIMEOUT.toSeconds() * 2, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 실행을 기다리다 중단됐다", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("동시 실행한 요청이 실패했다", e);
        } finally {
            executor.shutdownNow();
        }
    }
}
