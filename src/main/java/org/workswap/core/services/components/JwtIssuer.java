package org.workswap.core.services.components;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.User;

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

    public String issue(User user, Collection<? extends GrantedAuthority> authorities) throws JOSEException {

        Instant now = Instant.now();
        JWSSigner signer = new RSASSASigner(rsaKey);
        Map<String, Object> claims = new HashMap<>();

        claims.put("email", user.getEmail());
        claims.put("uid", user.getId());
        claims.put("roles", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .toArray(String[]::new));
        claims.put("permissions", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> !a.startsWith("ROLE_"))
            .toArray(String[]::new));

        JWTClaimsSet set = new JWTClaimsSet.Builder()
            .subject(user.getEmail())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(Duration.ofMinutes(30)))) // TTL 30 мин
            .claim("roles", claims.get("roles"))
            .claim("permissions", claims.get("permissions"))
            .jwtID(UUID.randomUUID().toString())
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.getKeyID()).type(JOSEObjectType.JWT).build();

        SignedJWT jwt = new SignedJWT(header, set);
        jwt.sign(signer);
        return jwt.serialize();
    }
}
