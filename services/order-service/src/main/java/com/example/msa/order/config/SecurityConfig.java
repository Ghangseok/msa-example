package com.example.msa.order.config;

import com.example.msa.order.exception.ProblemAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 주문 서비스가 JWT를 직접 검증한다(OAuth2 리소스 서버). 서명, 만료, 발급자를 검증한다.
 * 발급자 주소는 설정 {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}에 있다.
 *
 * <ul>
 *   <li>{@code POST /orders}는 인증된 사용자만 부른다. 역할과 관계없이 로그인한 사용자는 주문할 수 있다.
 *   <li>health 엔드포인트는 인증 없이 연다. 쿠버네티스 프로브가 토큰 없이 부르기 때문이다.
 *   <li>인증에 실패하면 {@link ProblemAuthenticationEntryPoint}가 401 Problem Details(code = UNAUTHORIZED)로 낸다.
 *   <li>세션을 만들지 않는다. 남아야 하는 데이터를 Pod 메모리에 두지 않기 위해서다. 토큰으로만 요청을 확인하므로 CSRF 보호도 끈다.
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint) {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/orders")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
        try {
            return http.build();
        } catch (Exception e) {
            // HttpSecurity.build()가 검사 예외(Exception)를 선언한다. 보안 설정을 만들지 못하면 앱을 띄우지 않는다.
            throw new IllegalStateException("보안 필터 체인을 만들지 못했다", e);
        }
    }
}
