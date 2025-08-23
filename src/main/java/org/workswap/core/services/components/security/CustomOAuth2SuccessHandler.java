package org.workswap.core.services.components.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.workswap.core.services.UserService;
import org.workswap.datasource.central.model.User;

import com.nimbusds.jose.JOSEException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtIssuer jwtIssuer; // твой сервис для access JWT
    private final UserService userService;

    @Value("${backoffice.url}")
    private String backofficeUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        User user = userService.findUser(email);
        if (user == null) {
            response.sendRedirect(backofficeUrl + "/register?email=" 
                    + URLEncoder.encode(email, StandardCharsets.UTF_8));
            return;
        }

        String accessToken;
        String refreshToken;
        try {
            accessToken = jwtIssuer.issueAccessToken(user);
            refreshToken = jwtIssuer.issueRefreshToken(user);
        } catch (JOSEException e) {
            throw new ServletException("Ошибка генерации JWT", e);
        }

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        // локально HTTPS скорее всего нет → временно false
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/"); // шире, чтобы работало и для /api/auth/refresh
        refreshCookie.setMaxAge((int) Duration.ofDays(30).getSeconds());

        // SameSite=None → браузер будет слать в cross-origin запросах
        String cookie = String.format(
            "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
            refreshToken,
            Duration.ofDays(30).getSeconds()
        );

        // на проде добавь `; Secure`
        response.setHeader("Set-Cookie", cookie);

        // редиректим на фронт (например, React на 3000 порту)
        response.sendRedirect(backofficeUrl + "/login/success?accessToken=" + accessToken);
    }   
}