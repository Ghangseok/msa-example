package com.example.msa.order.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * 검증한 JWT를 인증 정보로 바꾼다. Keycloak의 realm 역할({@code realm_access.roles})을 Spring Security 권한으로 만든다.
 * 역할 이름마다 {@code ROLE_} 접두어를 붙인다(예: {@code ADMIN} → {@code ROLE_ADMIN}). 그래야 {@code hasRole("ADMIN")}이 찾는다.
 * Spring Security의 기본 변환기는 {@code scope} 값만 읽어서 realm 역할을 권한으로 바꾸지 못한다.
 * {@code realm_access}나 {@code roles}가 없으면 권한이 없고, 글자가 아닌 원소는 무시한다. 인증 정보의 principal은 기본 변환기와 같이 {@link Jwt}다.
 */
public class RealmRoleJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

    public RealmRoleJwtConverter() {
        delegate.setJwtGrantedAuthoritiesConverter(RealmRoleJwtConverter::authoritiesOf);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return delegate.convert(jwt);
    }

    private static Collection<GrantedAuthority> authoritiesOf(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }
}
