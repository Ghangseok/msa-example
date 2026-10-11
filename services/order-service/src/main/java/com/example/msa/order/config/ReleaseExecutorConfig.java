package com.example.msa.order.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 해제 실행기를 만든다. 실패한 주문의 예약 해제를 고객 응답과 따로 돌리는 스레드 풀이다.
 * 고객 응답이 해제를 기다리지 않게 하려는 것이다.
 *
 * <p>종료할 때는 실행 중인 해제를 정해진 시간까지 기다린다. 그래도 인스턴스가 죽으면 해제 결과가 빈 "실패" 주문이 남는다.
 * 이것은 설계 문서 3절이 적어 둔 알려진 한계다. 해제 대기열은 Pod 메모리에 있지만 남아야 하는 데이터는 아니다.
 * 해제할 주문은 DB의 "실패" 주문에서 다시 찾을 수 있기 때문이다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReleaseExecutorProperties.class)
public class ReleaseExecutorConfig {

    @Bean(name = "releaseExecutor")
    ThreadPoolTaskExecutor releaseExecutor(ReleaseExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("release-");
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds((int) properties.awaitTermination().toSeconds());
        return executor;
    }
}
