package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 스토리 3: 로그인하지 않은 사람은 주문을 조회할 수 없다. 시나리오 3.1이다.
 *
 * <p>Spring Security Test의 {@code jwt()}는 토큰 검증을 건너뛰므로 이 테스트에는 쓰지 않는다.
 * {@code InvalidTokenIntegrationTest}와 같은 방법으로, 테스트용 RSA 키 쌍으로 토큰을 직접 만들고 이 테스트에서만 주문 서비스가
 * 테스트용 공개 키로 토큰을 검증하게 설정한다. 그 클래스의 도우미는 {@code private}이라서 이 클래스에 따로 둔다.
 */
class UnauthenticatedViewIntegrationTest extends ViewTestSupport {

    private static final KeyPair TRUSTED_KEYS = newKeyPair();
    private static final KeyPair OTHER_KEYS = newKeyPair();

    /** 이 테스트에서만, 주문 서비스가 믿는 공개 키를 테스트용 키로 바꾼다. 발급자 주소로 키를 받아 오지 않는다. */
    @TestConfiguration(proxyBeanMethods = false)
    static class TrustedKeyConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TRUSTED_KEYS.getPublic())
                    .build();
        }
    }

    @Test
    @DisplayName("TC-025 토큰이 없거나 서명이 틀리거나 만료된 토큰으로는 주문을 조회할 수 없다")
    void viewWithoutValidTokenIsUnauthorized() {
        // When 토큰 없이 주문 목록을 조회한다
        Reply list = get(null, "/orders");

        // Then 401을 받는다 (오류 응답 본문은 Problem Details 형식이고 code는 UNAUTHORIZED다. 공통 전제)
        assertUnauthorized(list);
        assertThat(list.text("$.code")).isEqualTo("UNAUTHORIZED");

        // When 토큰 없이 주문 하나를 조회한다
        Reply one = get(null, "/orders/1");

        // Then 401을 받는다
        assertUnauthorized(one);
        assertThat(one.text("$.code")).isEqualTo("UNAUTHORIZED");

        // When 토큰 없이 관리자 조회를 한다
        Reply admin = get(null, "/admin/orders");

        // Then 401을 받는다
        assertUnauthorized(admin);
        assertThat(admin.text("$.code")).isEqualTo("UNAUTHORIZED");

        // When 서명이 틀린 토큰으로 주문 목록을 조회한다
        Reply wrongSignature = get(bearer(token(OTHER_KEYS, Instant.now().plus(Duration.ofHours(1)))), "/orders");

        // Then 401을 받는다
        assertUnauthorized(wrongSignature);

        // When 만료된 토큰으로 주문 목록을 조회한다
        Reply expired = get(bearer(token(TRUSTED_KEYS, Instant.now().minus(Duration.ofHours(1)))), "/orders");

        // Then 401을 받는다
        assertUnauthorized(expired);
    }

    private static void assertUnauthorized(Reply reply) {
        assertThat(reply.status()).isEqualTo(401);
        assertThat(reply.contentType()).startsWith("application/problem+json");
    }

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    /** {@code keys}의 개인 키로 서명한 토큰을 만든다. {@code expiresAt}이 만료 시각이다. */
    private static String token(KeyPair keys, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(C1)
                    .issueTime(Date.from(Instant.now().minus(Duration.ofHours(2))))
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("테스트용 토큰에 서명하지 못했다", e);
        }
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("테스트용 RSA 키 쌍을 만들지 못했다", e);
        }
    }
}
