package org.workswap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.workswap.core.services.components.security.AuthChannelInterceptor;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
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

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    /* private class JwtHandshakeHandler extends DefaultHandshakeHandler {

        private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
        
        @Override
        protected Principal determineUser(
                @NonNull ServerHttpRequest request,
                @NonNull WebSocketHandler wsHandler,
                @NonNull Map<String, Object> attributes
        ) {
            List<String> authHeaders = request.getHeaders().get("Authorization");
            logger.debug("authHeaders: {}", authHeaders);
            if (authHeaders == null || authHeaders.isEmpty()) return null;

            String bearer = authHeaders.get(0);
            logger.debug("bearer: {}", bearer);
            if (!bearer.startsWith("Bearer ")) return null;

            String token = bearer.substring(7);
            String email = jwtService.validateAndGetEmail(token);
            return () -> email;
        }

        /* private String extractTokenFromUri(String uri) {
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
    } */
}
