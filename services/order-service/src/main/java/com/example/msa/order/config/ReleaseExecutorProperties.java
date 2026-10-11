package com.example.msa.order.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 해제 실행기(실패한 주문의 예약 해제를 고객 응답과 따로 돌리는 스레드 풀) 설정이다.
 * 값은 application.yml의 {@code release-executor}에 있고, 코드에는 적지 않는다.
 *
 * @param corePoolSize 늘 떠 있는 스레드 수
 * @param maxPoolSize 대기열이 가득 찼을 때까지 늘릴 수 있는 최대 스레드 수
 * @param queueCapacity 대기열 크기
 * @param awaitTermination 종료할 때 실행 중인 해제를 기다리는 최대 시간
 */
@ConfigurationProperties("release-executor")
public record ReleaseExecutorProperties(
        int corePoolSize, int maxPoolSize, int queueCapacity, Duration awaitTermination) {}
