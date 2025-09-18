package org.workswap.core.services.components.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtIssuer {
    
    private final RSAKey rsaKey;

    public String issueAccessToken(User user) throws JOSEException {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        Instant now = Instant.now();
        JWSSigner signer = new RSASSASigner(rsaKey);
        Map<String, Object> claims = new HashMap<>();

        // Основные данные пользователя
        claims.put("email", user.getEmail());
        claims.put("uid", user.getId());

        // Формируем роли и пермишены из User → Role → Permission
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            for (Permission perm : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(perm.getName().toUpperCase()));
            }
        }

        // Отдельно вытаскиваем роли и пермишены в claims
        String[] roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toArray(String[]::new);

        String[] permissions = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toArray(String[]::new);

        claims.put("roles", roles);
        claims.put("permissions", permissions);

        // Строим JWT
        JWTClaimsSet set = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(30)))) // TTL 30 мин (для теста 3 минуты, вернуть на 30) 
                .claim("roles", roles)
                .claim("permissions", permissions)
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, set);
        jwt.sign(signer);
        return jwt.serialize();
    }

    public String issueRefreshToken(User user) throws JOSEException {

        Instant now = Instant.now();
        JWSSigner signer = new RSASSASigner(rsaKey);

        // Клеймы минимальны: только идентификация
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());

        JWTClaimsSet set = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofDays(30)))) // TTL 30 дней
                .claim("uid", claims.get("uid"))
                .claim("email", claims.get("email"))
                .claim("typ", "refresh") // пометка типа токена
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, set);
        jwt.sign(signer);

        return jwt.serialize();
    }
}
