package com.example.msa.order.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.order.config.InventoryClientProperties;
import io.github.resilience4j.core.functions.Either;
import java.io.IOException;
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
 * 재시도 전 대기 일정이 몇 번째 재시도인지에 따라 대기를 맞게 돌려주는지 본다. Spring 앱을 띄우지 않는다.
 * 줄이지 않은 기본 설정({@code application.yml}만 읽는다. 테스트 설정은 섞지 않는다)으로 확인한다.
 *
 * <p>기대값의 출처는 비기능 요구사항 문서(docs/requirements/non-functional.md)의 "장애 전파 차단" 목표값
 * ("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답")의
 * "재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤)"다. 첫째부터 다섯째 재시도 전 대기는 0, 1000, 2000, 4000, 8000밀리초다.
 * 대기 일정은 인수 시나리오 테스트(시나리오 8.1)를 받치는 테스트라서 테스트 케이스 번호를 붙이지 않는다.
 */
class RetryScheduleTest {

    @Test
    @DisplayName("기본 설정에서 첫째부터 다섯째 재시도 전 대기는 0, 1000, 2000, 4000, 8000밀리초다")
    void defaultWaitsBeforeFirstThroughFifthRetry() throws IOException {
        RetrySchedule schedule = new RetrySchedule(bindDefaults().retryWaits());

        assertThat(schedule.waitMillisBeforeRetry(1)).isEqualTo(0L);
        assertThat(schedule.waitMillisBeforeRetry(2)).isEqualTo(1000L);
        assertThat(schedule.waitMillisBeforeRetry(3)).isEqualTo(2000L);
        assertThat(schedule.waitMillisBeforeRetry(4)).isEqualTo(4000L);
        assertThat(schedule.waitMillisBeforeRetry(5)).isEqualTo(8000L);
    }

    @Test
    @DisplayName("Resilience4j에 넘기는 간격 함수도 지금까지 시도한 횟수에 따라 같은 대기를 돌려준다")
    void intervalFunctionReturnsTheSameWaits() throws IOException {
        RetrySchedule schedule = new RetrySchedule(bindDefaults().retryWaits());
        Either<Throwable, Object> failed = Either.left(new IllegalStateException("일시 오류"));

        // Retry는 n번 시도한 뒤 n번째 재시도 전의 대기를 묻는다.
        assertThat(List.of(1, 2, 3, 4, 5).stream()
                        .map(attempts -> schedule.intervalFunction().apply(attempts, failed))
                        .toList())
                .containsExactly(0L, 1000L, 2000L, 4000L, 8000L);
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
