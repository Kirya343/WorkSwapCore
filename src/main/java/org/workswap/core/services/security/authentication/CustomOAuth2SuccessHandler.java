package org.workswap.core.services.security.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.common.enums.UserStatus;
import org.workswap.core.services.command.UserCommandService;
import org.workswap.core.services.security.AuthCookiesService;
import org.workswap.core.services.query.PermissionQueryService;
import org.workswap.core.services.query.UserQueryService;
import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Profile("production")
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    private final UserRepository userRepository;

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final PermissionQueryService permissionQueryService;

    private final AuthCookiesService cookiesService;

    @Value("${backoffice.url}")
    private String backofficeUrl;
    
    @Override
    @Transactional
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws ServletException, IOException  {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String googleAvatar = oidcUser.getPicture();

        boolean newUser = false;

        User user = userQueryService.findUser(email);

        if (user == null || user.getStatus() == UserStatus.PENDING) {
            newUser = true;

            if (user == null) {
                Role role = permissionQueryService.findRole("USER");

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

        String tempUserId = request.getSession().getAttribute("tempUserId").toString();

        Set<Listing> favotires = importTempUser(tempUserId, user);

        user.getFavoriteListings().addAll(favotires);

        user.getSettings().setGoogleAvatar(googleAvatar);
        userCommandService.save(user);

        String encodedRedirect = (String) request.getSession().getAttribute("redirectUrl");
        logger.debug("encodedRedirect: " + encodedRedirect);

        String redirectUrl = "/";

        redirectUrl = decodeRedirect(encodedRedirect, redirectUrl);

        RedirectData data = getRedirectPath(redirectUrl);

        cookiesService.setAuthCookies(response, user);

        String redirectQuery = "?redirect=" + URLEncoder.encode(data.pathAndQuery, StandardCharsets.UTF_8);

        if (newUser == false) {
            response.sendRedirect(data.domain + "/login/success" + redirectQuery);
        } else {
            response.sendRedirect(data.domain + "/register" + redirectQuery);
        }
    }

    private Set<Listing> importTempUser(String tempUserId, User user) {
        User tempUser = userQueryService.findUser(tempUserId);

        Set<Listing> favorites = new HashSet<>(tempUser.getFavoriteListings());

        userRepository.deleteById(Long.valueOf(tempUserId));

        return favorites;
    }

    private String decodeRedirect(String encodedRedirect, String redirectUrl) {

        if (encodedRedirect != null && !encodedRedirect.isEmpty()) {

            String decodedState = URLDecoder.decode(encodedRedirect, StandardCharsets.UTF_8);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(decodedState);
            redirectUrl = new String(decodedBytes, StandardCharsets.UTF_8);

            logger.debug("redirectUrl: {}", redirectUrl);
            
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