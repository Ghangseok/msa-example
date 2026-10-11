package com.example.msa.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 재고 서비스를 부르는 {@link RestClient}와 서킷 브레이커를 만든다.
 *
 * <p>{@code RestClient}는 Spring Boot가 주는 {@code RestClient.Builder}로 만든다. 그래야 추적 정보(traceparent)가
 * 요청에 자동으로 붙는다. {@code RestClient}의 정적 팩토리 메서드로 직접 만들지 않는다.
 * 요청 팩토리는 별도 의존성이 없는 {@link JdkClientHttpRequestFactory}이고, 연결 제한 시간과 요청 제한 시간은
 * {@link InventoryClientProperties}에서 넣는다. 값은 코드에 적지 않는다.
 *
 * <p>서킷 브레이커는 재고 호출 전체에 하나(이름 inventory)다. 예약과 해제가 함께 쓴다. 실패로 세는 것은 연결 실패,
 * 시도 제한 시간 초과, 5xx이고, 업무 결과(RESERVED, REJECTED, RELEASED)와 4xx는 성공으로 센다. 이 분류는
 * {@code InventoryClient}가 시도마다 결과를 기록하면서 정한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InventoryClientProperties.class)
public class InventoryClientConfig {

    static final String CIRCUIT_BREAKER_NAME = "inventory";

    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder, InventoryClientProperties properties) {
        // 클러스터 안의 평문 HTTP 호출이다. JDK HttpClient의 기본 HTTP/2 업그레이드(h2c)를 쓰지 않고 HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());
        return builder.baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    CircuitBreaker inventoryCircuitBreaker(InventoryClientProperties properties) {
        InventoryClientProperties.CircuitBreakerProperties circuit = properties.circuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(circuit.slidingWindowType())
                .slidingWindowSize(circuit.slidingWindowSize())
                .minimumNumberOfCalls(circuit.minimumNumberOfCalls())
                .failureRateThreshold(circuit.failureRateThreshold())
                .waitDurationInOpenState(circuit.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(circuit.permittedNumberOfCallsInHalfOpenState())
                .build();
        return CircuitBreaker.of(CIRCUIT_BREAKER_NAME, config);
    }
}
