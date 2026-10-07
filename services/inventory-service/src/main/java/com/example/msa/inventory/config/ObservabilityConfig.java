package com.example.msa.inventory.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Logback의 OpenTelemetry appender에 OpenTelemetry 객체를 연결한다.
 * Spring Boot는 이 연결을 자동으로 하지 않는다(Spring Boot 4.1 문서 "Loggers" 절의 OpenTelemetry Logging).
 */
@Configuration(proxyBeanMethods = false)
public class ObservabilityConfig {

    @Bean
    InitializingBean openTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
