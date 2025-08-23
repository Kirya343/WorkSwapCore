package org.workswap.core.services.components.security;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
        return customize(req, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
        return customize(req, request);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req, HttpServletRequest request) {

        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Original OAuth2 request: " + req);
        if (req == null) return null;

        // Берём redirect URL из query-параметра
        String redirect = request.getParameter("redirect");
        Map<String, Object> additionalParams = new HashMap<>(req.getAdditionalParameters());
        if (redirect != null) {
            additionalParams.put("redirect", redirect);
        }

        // Создаём новый OAuth2AuthorizationRequest с дополнительными параметрами
        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(additionalParams)
                .build();
    }
}