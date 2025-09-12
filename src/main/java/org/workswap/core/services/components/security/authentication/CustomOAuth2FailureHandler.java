package org.workswap.core.services.components.security.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2FailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String defaultRedirect = "https://workswap.org/";

        String encodedRedirect = (String) request.getSession().getAttribute("redirectUrl");
        logger.debug("encodedRedirect: {}", encodedRedirect);

        String redirectUrl = decodeRedirect(encodedRedirect, defaultRedirect);

        RedirectData data = getRedirectPath(redirectUrl);

        String redirectQuery = "?redirect=" + URLEncoder.encode(data.pathAndQuery, StandardCharsets.UTF_8) + "&error=invalid_creditails";

        response.sendRedirect(data.domain + "/login" + redirectQuery);
    }

    private String decodeRedirect(String encodedRedirect, String defaultRedirect) {

        if (encodedRedirect != null && !encodedRedirect.isEmpty()) {
            try {
                String decodedState = URLDecoder.decode(encodedRedirect, StandardCharsets.UTF_8);
                byte[] decodedBytes = Base64.getUrlDecoder().decode(decodedState);
                return new String(decodedBytes, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                logger.warn("Не удалось декодировать redirect, используем default", e);
            }
        }

        return defaultRedirect;
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