package org.workswap.core.services.components.security.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

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
import org.workswap.common.enums.UserStatus;
import org.workswap.core.services.RoleService;
import org.workswap.core.services.command.UserCommandService;
import org.workswap.core.services.components.security.JwtIssuer;
import org.workswap.core.services.query.UserQueryService;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.user.Role;

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

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final RoleService roleService;

    @Value("${backoffice.url}")
    private String backofficeUrl;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.sameSite}")
    private String cookieSameSite;
    
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String googleAvatar = oidcUser.getPicture();

        boolean newUser = false;

        User user = userQueryService.findUser(email);

        if (user == null || user.getStatus() == UserStatus.PENDING) {
            newUser = true;

            if (user == null) {
                Role role = roleService.getRoleByName("USER");

                Set<Role> roles = new HashSet<>();

                roles.add(role);
                
                user = new User(
                    oidcUser.getClaim("name"),
                    email,
                    oidcUser.getSubject(),
                    googleAvatar,
                    roles,
                    false
                );
            }
        }

        user = userCommandService.save(user);

        user.getSettings().setGoogleAvatar(googleAvatar);
        userCommandService.save(user);

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

        String encodedRedirect = (String) request.getSession().getAttribute("redirectUrl");
        logger.debug("encodedRedirect: " + encodedRedirect);

        String redirectUrl = "/";

        redirectUrl = decodeRedirect(encodedRedirect, redirectUrl);

        RedirectData data = getRedirectPath(redirectUrl);

        ResponseCookie cookie = setRefreshCookie(response, refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String redirectQuery = "?redirect=" + URLEncoder.encode(data.pathAndQuery, StandardCharsets.UTF_8);

        if (newUser == false) {
            response.sendRedirect(data.domain + "/login/success" + redirectQuery);
        } else {
            response.sendRedirect(data.domain + "/register" + redirectQuery);
        }
    }   

    private ResponseCookie setRefreshCookie(HttpServletResponse response, String refreshToken) {

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite(cookieSameSite)
            .domain(cookieDomain.isEmpty() ? null : cookieDomain)
            .maxAge(Duration.ofDays(30))
            .build();

        return cookie;
    }

    private String decodeRedirect(String encodedRedirect, String redirectUrl) {

        if (encodedRedirect != null && !encodedRedirect.isEmpty()) {

            String decodedState = URLDecoder.decode(encodedRedirect, StandardCharsets.UTF_8);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(decodedState);
            redirectUrl = new String(decodedBytes, StandardCharsets.UTF_8);

            logger.debug("redirectUrl: " + redirectUrl);
            
            return redirectUrl;
        }

        return null;
    }

    private RedirectData getRedirectPath(String redirectUrl) {

        RedirectData data = new RedirectData();

        try {
            logger.debug("redirectUrl: {}", redirectUrl);
            URI uri = new URI(redirectUrl);

            logger.debug("uri: {}", uri);

            // Домен + порт + схема
            data.domain = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) data.domain += ":" + uri.getPort();

            // Путь + query
            data.pathAndQuery = uri.getRawPath();
            if (uri.getRawQuery() != null) data.pathAndQuery += "?" + uri.getRawQuery();

            logger.debug("Domain: " + data.domain);
            logger.debug("Path + query: " + data.pathAndQuery);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static class RedirectData {
        String domain = "https://workswap.org";
        String pathAndQuery = "/";
    }
}