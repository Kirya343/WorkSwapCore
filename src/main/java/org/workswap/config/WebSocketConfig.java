package org.workswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.workswap.core.services.components.security.JwtService;

import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new JwtHandshakeHandler());
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue"); // добавь /queue, если используешь @SendToUser
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public boolean configureMessageConverters(@NonNull List<MessageConverter> messageConverters) {
        messageConverters.add(new MappingJackson2MessageConverter());
        return false;
    }

    private class JwtHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(
                @NonNull ServerHttpRequest request,
                @NonNull WebSocketHandler wsHandler,
                @NonNull Map<String, Object> attributes
        ) {
            // Достаём токен из параметра
            String uri = request.getURI().toString();
            String token = extractTokenFromUri(uri);

            if (token == null) {
                return null; // Нет токена — анонимный пользователь
            }

            // Валидируем токен, получаем username
            String email = jwtService.validateAndGetEmail(token);
            return () -> email; // Возвращаем Principal
        }

        private String extractTokenFromUri(String uri) {
            // Простая реализация — достаём параметр из URL
            try {
                Map<String, String> params = splitQuery(new URI(uri));
                return params.get("access_token");
            } catch (URISyntaxException e) {
                return null;
            }
        }

        private Map<String, String> splitQuery(URI uri) {
            Map<String, String> params = new LinkedHashMap<>();
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        params.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                }
            }
            return params;
        }
    }
}
