package org.workswap.core.services.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.User;

import com.nimbusds.jose.JOSEException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class AuthCookiesService {

    private static final Logger logger = LoggerFactory.getLogger(AuthCookiesService.class);

    private final JwtIssuer jwtIssuer;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.sameSite}")
    private String cookieSameSite;
    
    public void setAuthCookies(HttpServletResponse response, User user) throws ServletException {

        String accessToken;
        String refreshToken;

        try {
            logger.debug("Генерируем токены для: {}", user.getName());
            accessToken = jwtIssuer.issueAccessToken(user);
            refreshToken = jwtIssuer.issueRefreshToken(user);
        } catch (JOSEException e) {
            throw new ServletException("Ошибка генерации JWT", e);
        }

        logger.debug("accessToken: {}", accessToken);
        logger.debug("refreshToken: {}", refreshToken);

        ResponseCookie refreshCookie = setTokenCookie("refreshToken", refreshToken, Duration.ofDays(30));
        ResponseCookie accessCookie = setTokenCookie("accessToken", accessToken, Duration.ofMinutes(15));

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void deleteAuthCookies(HttpServletResponse response) throws ServletException {

        ResponseCookie refreshCookie = setTokenCookie("refreshToken", "", Duration.ZERO);
        ResponseCookie accessCookie = setTokenCookie("accessToken", "", Duration.ZERO);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private ResponseCookie setTokenCookie(String name, String token, Duration duration) {

        ResponseCookie cookie = ResponseCookie.from(name, token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite(cookieSameSite)
            .domain(cookieDomain.isEmpty() ? null : cookieDomain)
            .maxAge(duration)
            .build();

        return cookie;
    }
}