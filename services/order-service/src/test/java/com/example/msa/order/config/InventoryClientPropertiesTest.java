package com.example.msa.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * 재고 호출 설정의 기본값이 맞게 들어갔는지 본다. Spring 앱을 띄우지 않는다.
 * 서비스 통합 테스트는 시간 값을 1/5로 줄인 설정({@code application-test.yml})으로 도므로, 줄이지 않은 기본값은 이 테스트가 따로 확인한다.
 * 테스트 설정이 덮어쓰지 않은 {@code application.yml}만 읽는다.
 *
 * <p>기대값의 출처는 두 가지다. 시도당 제한 시간 2.5초는 비기능 요구사항 문서(docs/requirements/non-functional.md)의
 * "장애 전파 차단" 목표값("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답")이다.
 * 서킷 브레이커 값은 설계 문서(docs/design/architecture.md) 4절 표의 값이다. 최근 10번 중 50% 이상 실패하면(최소 10번 기록된 뒤) 열리고,
 * 열려 있는 시간은 10초이며, 그 뒤 시험 호출 3번을 보낸다.
 * 재시도 최대 횟수, 재시도 전 대기, 전체 한도는 위 목표값 가운데 "재시도 포함 전체 30초"와 "재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤)"다.
 */
class InventoryClientPropertiesTest {

    @Test
    @DisplayName("시도당 연결 제한 시간과 요청 제한 시간의 기본값은 2.5초다")
    void attemptTimeoutsAre2500Milliseconds() throws IOException {
        InventoryClientProperties properties = bindDefaults();

        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(2500));
        assertThat(properties.requestTimeout()).isEqualTo(Duration.ofMillis(2500));
    }

    @Test
    @DisplayName("서킷 브레이커의 기본값은 설계 4절 표의 값이다: 최근 10번, 최소 10번 기록, 실패율 50%, 열려 있는 시간 10초, 시험 호출 3번")
    void circuitBreakerValuesFollowDesignTable() throws IOException {
        InventoryClientProperties.CircuitBreakerProperties circuit =
                bindDefaults().circuitBreaker();

        assertThat(circuit.slidingWindowSize()).isEqualTo(10);
        assertThat(circuit.minimumNumberOfCalls()).isEqualTo(10);
        assertThat(circuit.failureRateThreshold()).isEqualTo(50f);
        assertThat(circuit.waitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(circuit.permittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    @DisplayName("재시도는 최대 5번이고, 재시도 전 대기의 기본값은 즉시, 1, 2, 4, 8초다")
    void retryCountAndWaitsFollowTargetValues() throws IOException {
        InventoryClientProperties properties = bindDefaults();

        assertThat(properties.maxRetries()).isEqualTo(5);
        assertThat(properties.retryWaits())
                .containsExactly(
                        Duration.ZERO,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(4),
                        Duration.ofSeconds(8));
    }

    @Test
    @DisplayName("재시도를 포함한 전체 한도의 기본값은 30초다")
    void overallTimeLimitIs30Seconds() throws IOException {
        assertThat(bindDefaults().overallTimeLimit()).isEqualTo(Duration.ofSeconds(30));
    }

    /** 테스트 설정을 섞지 않고 application.yml만 읽어 설정 클래스에 묶는다. 환경변수는 쓰지 않아서 기본값이 그대로 나온다. */
    private static InventoryClientProperties bindDefaults() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        MutablePropertySources propertySources = new MutablePropertySources();
        sources.forEach(propertySources::addLast);
        Binder binder = new Binder(
                ConfigurationPropertySources.from(propertySources),
                new PropertySourcesPlaceholdersResolver(propertySources));
        return binder.bind("inventory-client", InventoryClientProperties.class).get();
    }
}
