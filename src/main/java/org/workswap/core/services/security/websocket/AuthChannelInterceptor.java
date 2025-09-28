package org.workswap.core.services.security.websocket;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.workswap.core.services.analytic.OnlineCounter;
import org.workswap.core.services.security.JwtService;

import lombok.RequiredArgsConstructor;

@Component
@Profile("production")
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    private final JwtService jwtService;
    private final OnlineCounter onlineCounter;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes == null) {
                logger.error("No session attributes in WebSocket CONNECT");
                throw new MessagingException("Missing session attributes");
            }
            
            String token = (String) sessionAttributes.get("accessToken");

            if (token == null) {
                logger.debug("No accessToken in WebSocket handshake");
                throw new MessagingException("Missing access token");
            }

            try {
                String userId = jwtService.validateAndGetUserIdStr(token);
                if (userId == null) {
                    throw new MessagingException("Invalid or expired token");
                }

                Collection<GrantedAuthority> authorities = jwtService.getAuthorities(token);

                Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

                accessor.setUser(authentication);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                onlineCounter.increment();

                logger.debug("STOMP user authenticated: {}", context);

            } catch (Exception e) {
                logger.error("STOMP token validation failed: {}", e.getMessage());
                throw new MessagingException("Invalid token");
            }
        }

        return message;
    }

    @Override
    public void postSend(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();

        if (command != null) {
            switch (command) {
                case DISCONNECT:
                    onlineCounter.decrement();
                    break;
                default:
                    break;
            }
        }
    }
}