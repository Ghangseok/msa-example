package com.example.msa.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 역할 변환기가 토큰의 {@code realm_access.roles}를 Spring Security 권한으로 바꾸는지 본다.
 * 기대값은 {@code docs/design/architecture.md} 7절의 "역할은 Keycloak realm 역할(CUSTOMER, ADMIN)을 Spring Security 권한으로 바꿔 쓴다"와
 * 시나리오 4.1의 "관리자가 아닌 사람은 403"이다. 판정은 {@code hasRole("ADMIN")}으로 한다.
 * 권한 이름의 {@code ROLE_} 접두어 글자는 문서에 기대값이 없어서 단언하지 않는다.
 * 토큰의 {@code realm_access.roles} 모양은 이 테스트가 입력으로만 쓴다. 실제 Keycloak 토큰이 이 모양인지는 티켓 003의 E2E에서 확인한다.
 */
class RealmRoleJwtConverterTest {

    private final RealmRoleJwtConverter converter = new RealmRoleJwtConverter();

    @Test
    @DisplayName("realm_access.roles에 ADMIN이 있으면 hasRole(\"ADMIN\") 판정을 통과한다")
    void adminRoleIsGranted() {
        assertThat(isAdmin(converter.convert(token(Map.of("roles", List.of("ADMIN"))))))
                .isTrue();
    }

    @Test
    @DisplayName("realm_access.roles에 CUSTOMER만 있으면 hasRole(\"ADMIN\") 판정을 통과하지 못한다")
    void customerRoleIsNotGranted() {
        assertThat(isAdmin(converter.convert(token(Map.of("roles", List.of("CUSTOMER"))))))
                .isFalse();
    }

    @Test
    @DisplayName("realm_access 클레임이 없는 토큰은 hasRole(\"ADMIN\") 판정을 통과하지 못한다")
    void tokenWithoutRealmAccessIsNotGranted() {
        assertThat(isAdmin(converter.convert(token(null)))).isFalse();
    }

    private static boolean isAdmin(Authentication authentication) {
        AuthorizationResult result =
                AuthorityAuthorizationManager.hasRole("ADMIN").authorize(() -> authentication, null);
        return result != null && result.isGranted();
    }

    /** {@code realmAccess}가 null이면 {@code realm_access} 클레임이 없는 토큰이다. */
    private static Jwt token(Map<String, Object> realmAccess) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("someone")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (realmAccess != null) {
            builder.claim("realm_access", realmAccess);
        }
        return builder.build();
    }
}
