package org.workswap.core.services.components.security;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

@Profile({"production", "backoffice"})
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

        String encodedRedirect = (String) request.getSession().getAttribute("redirectUrl");
        logger.debug("encodedRedirect: " + encodedRedirect);

        String redirectUrl = "/";
        if (encodedRedirect != null && !encodedRedirect.isEmpty()) {
            String decodedState = URLDecoder.decode(encodedRedirect, StandardCharsets.UTF_8);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(decodedState);
            redirectUrl = new String(decodedBytes, StandardCharsets.UTF_8);

            logger.debug("redirectUrl: " + redirectUrl);
        }

        String domain = "https://workswap.org";
        String pathAndQuery = "/";
        try {
            logger.debug("redirectUrl: {}", redirectUrl);
            URI uri = new URI(redirectUrl);

            logger.debug("uri: {}", uri);

            // Домен + порт + схема
            domain = uri.getScheme() + "://" + uri.getHost();
            int port = uri.getPort();
            if (port != -1) {
                domain += ":" + port;
            }

            // Путь + query
            pathAndQuery = uri.getRawPath(); // /catalog
            if (uri.getRawQuery() != null) {
                pathAndQuery += "?" + uri.getRawQuery(); // /catalog?category=123
            }

            logger.debug("Domain: " + domain);
            logger.debug("Path + query: " + pathAndQuery);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true) // обязательно для HTTPS
            .path("/")    // доступно для всего API
            .sameSite("None") // нужно для кросс-доменных запросов
            .maxAge(Duration.ofDays(30))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // редиректим на фронт (например, React на 3000 порту)
        response.sendRedirect(domain + "/login/success?accessToken=" + accessToken + "&redirect=" + URLEncoder.encode(pathAndQuery, StandardCharsets.UTF_8));
    }   
}