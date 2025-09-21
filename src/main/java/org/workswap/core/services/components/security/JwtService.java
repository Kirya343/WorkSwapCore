package org.workswap.core.services.components.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RSAKey rsaKey;

    /**
     * Проверяет токен, валидирует подпись и срок жизни
     * @param token строка JWT
     * @return claims (JWTClaimsSet), если токен валиден, иначе null
     */
    public JWTClaimsSet validate(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Проверяем подпись по публичному ключу
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                return null; // подпись не сошлась
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Проверяем срок жизни
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return null; // токен просрочен
            }

            return claims;

        } catch (ParseException | JOSEException e) {
            return null;
        }
    }

    /**
     * Валидирует токен и возвращает список GrantedAuthority
     * @param token JWT токен
     * @return список GrantedAuthority или пустой список, если токен невалиден
     * @throws ParseException 
     */
    public Collection<GrantedAuthority> getAuthorities(String token) throws ParseException {
        JWTClaimsSet claims = validate(token);
        if (claims == null) {
            return Collections.emptyList();
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // роли
        List<String> roles = claims.getStringListClaim("roles");
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }

        // права/permissions
        List<String> perms = claims.getStringListClaim("permissions");
        if (perms != null) {
            perms.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        }

        return authorities;
    }

    public String validateAndGetEmail(String token) {
        JWTClaimsSet claims = validate(token);
        return (claims != null) ? claims.getSubject() : null;
    }
}