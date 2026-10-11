package com.example.msa.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 재고 서비스 호출 설정이다. 값은 application.yml의 {@code inventory-client}에 있고, 코드에는 적지 않는다.
 *
 * @param baseUrl 재고 서비스 주소. 쿠버네티스 Service 이름으로 부른다
 * @param connectTimeout 시도 하나의 연결 제한 시간
 * @param requestTimeout 시도 하나의 요청 제한 시간
 * @param maxRetries 최초 시도 뒤 재시도의 최대 횟수. 최초 시도를 포함해 시도는 최대 {@code maxRetries + 1}번이다
 * @param retryWaits 재시도 전 대기. {@code n}번째 값이 {@code n}번째 재시도 전의 대기다. 길이는 {@code maxRetries}와 같다
 * @param overallTimeLimit 재시도를 포함한 전체 한도. 시작 시각부터 잰다
 * @param circuitBreaker 재고 호출의 서킷 브레이커 값
 */
@ConfigurationProperties("inventory-client")
public record InventoryClientProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxRetries,
        List<Duration> retryWaits,
        Duration overallTimeLimit,
        CircuitBreakerProperties circuitBreaker) {

    /**
     * 서킷 브레이커 값이다. 설계 문서 4절의 값을 Resilience4j 설정 이름으로 옮겼다.
     *
     * @param slidingWindowType 실패율을 셀 범위의 종류. 최근 호출 개수 기준(COUNT_BASED)이다
     * @param slidingWindowSize 실패율을 계산할 최근 시도 수
     * @param minimumNumberOfCalls 실패율을 계산하기 시작하는 최소 기록 수
     * @param failureRateThreshold 이 비율(%) 이상 실패하면 서킷이 열린다. 반쯤 열린 상태에서도 같은 기준을 쓴다
     * @param waitDurationInOpenState 서킷이 열려 있는 시간. 지나면 반쯤 열린 상태로 바뀐다
     * @param permittedNumberOfCallsInHalfOpenState 반쯤 열린 상태에서 보내는 시험 호출 수
     */
    public record CircuitBreakerProperties(
            SlidingWindowType slidingWindowType,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            Duration waitDurationInOpenState,
            int permittedNumberOfCallsInHalfOpenState) {}
}
