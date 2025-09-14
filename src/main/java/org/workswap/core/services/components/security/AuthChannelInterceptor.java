package org.workswap.core.services.components.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class AuthChannelInterceptor implements ChannelInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Токен приходит в STOMP заголовках, НЕ в HTTP заголовках
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            logger.debug("STOMP Authorization header: {}", authHeader != null ? "present" : "null");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email = jwtService.validateAndGetEmail(token);
                    accessor.setUser(() -> email);
                    logger.debug("STOMP user authenticated: {}", email);
                } catch (Exception e) {
                    logger.error("STOMP token validation failed: {}", e.getMessage());
                    throw new MessagingException("Invalid token");
                }
            } else {
                logger.debug("No Authorization header in STOMP CONNECT");
                throw new MessagingException("Missing Authorization header");
            }
        }
        
        return message;
    }
}