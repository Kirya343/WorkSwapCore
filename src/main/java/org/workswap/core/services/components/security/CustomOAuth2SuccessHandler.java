package org.workswap.core.services.components.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
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

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

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

        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        String redirectUrl = null;

        if (savedRequest != null) {
            logger.debug("savedRequest: {}", savedRequest);
            try {
                URI savedUri = new URI(savedRequest.getRedirectUrl());
                String query = savedUri.getQuery();
                if (query != null) {
                    Map<String, String> params = Arrays.stream(query.split("&"))
                        .map(s -> s.split("=", 2))
                        .filter(a -> a.length == 2)
                        .collect(Collectors.toMap(a -> a[0], a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)));
                    
                    if (params.containsKey("redirect")) {
                        redirectUrl = params.get("redirect");
                    }
                }
            } catch (Exception e) {
                logger.error("Ошибка при парсинге savedRequest URL", e);
            }
        } else {
            logger.debug("savedRequest == null");
            redirectUrl = request.getParameter("redirect");
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                redirectUrl = "/";
            }
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
        response.sendRedirect(backofficeUrl + "/login/success?accessToken=" + accessToken + "&redirect=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
    }   
}