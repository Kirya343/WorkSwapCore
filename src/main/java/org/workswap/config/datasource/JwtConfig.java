package org.workswap.config.datasource;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.jwk.RSAKey;

@Configuration
@EnableConfigurationProperties
public class JwtConfig {
    
    @Value("${jwt.private-key-location:classpath:jwt-private.pem}")
    private String privateKeyLocation;
    
    @Value("${jwt.public-key-location:classpath:jwt-public.pem}")  
    private String publicKeyLocation;
    
    @Bean
    public RSAKey rsaKey() throws Exception {
        // Загружаем приватный ключ
        RSAPrivateKey privateKey = loadPrivateKey();
        
        // Загружаем публичный ключ  
        RSAPublicKey publicKey = loadPublicKey();
        
        // Создаем RSAKey с обоими ключами
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString()) // Уникальный ID ключа
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        // Загружаем публичный ключ для валидации JWT
        RSAPublicKey publicKey = loadPublicKey();
        
        // Создаем JwtDecoder с публичным ключом
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
    
    private RSAPrivateKey loadPrivateKey() throws Exception {
        Resource resource = new ClassPathResource(privateKeyLocation.replace("classpath:", ""));
        String privateKeyPEM = new String(resource.getInputStream().readAllBytes());
        
        // Убираем заголовки и переносы строк
        privateKeyPEM = privateKeyPEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        // Декодируем Base64
        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        
        // Создаем ключ
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }
    
    private RSAPublicKey loadPublicKey() throws Exception {
        Resource resource = new ClassPathResource(publicKeyLocation.replace("classpath:", ""));
        String publicKeyPEM = new String(resource.getInputStream().readAllBytes());
        
        // Убираем заголовки и переносы строк
        publicKeyPEM = publicKeyPEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        // Декодируем Base64
        byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
        
        // Создаем ключ
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(spec);
    }
}
