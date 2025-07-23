package org.workswap.core.services.impl;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.News;
import org.workswap.datasource.central.model.Notification;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.DTOs.FullNotificationDTO;
import org.workswap.datasource.central.model.DTOs.NotificationDTO;
import org.workswap.datasource.central.model.enums.Importance;
import org.workswap.datasource.central.model.enums.NotificationType;
import org.workswap.datasource.central.repository.NotificationRepository;
import org.workswap.core.services.NewsService;
import org.workswap.core.services.NotificationService;
import org.workswap.core.services.UserService;
import org.workswap.core.someClasses.WebhookSigner;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final MessageSource messageSource;
    private final NewsService newsService;

    @Override
    public void saveOfflineChatNotification(String userParam, NotificationDTO dto) {

        User user = userService.findUser(userParam);

        Notification notification = new Notification(user, dto.getTitle(), dto.getMessage(), dto.getLink(), NotificationType.CHAT, Importance.INFO);
        notificationRepository.save(notification);

        // Отправляем в телеграмм
        try {
            // Строим JSON вручную или через ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("messageId", UUID.randomUUID().toString());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", user.getEmail());
            requestBody.put("message", dto.getMessage());
            requestBody.put("link", "https://workswap.org" + dto.getLink()); // добавил это
            requestBody.put("type", "info");
            requestBody.put("metadata", metadata);

            String json = objectMapper.writeValueAsString(requestBody);
            String signature = WebhookSigner.generateSignature(json);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://s1.qwer-host.xyz:25079/api/notifications/send"))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Notification sent. Status: " + response.statusCode());
                    System.out.println("Response: " + response.body());
                });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to send notification to Telegramm: " + e.getMessage());
        }
    }

    @Override
    public void sendNewsNotification(News news) {
        List<User> reciverList = userService.findAll(); 

        for(User receiver : reciverList) {

            Locale reciverLocale = Locale.of("en");
            
            if (!receiver.getLanguages().isEmpty()) {
                System.out.println("У пользователя найдено языков: " + receiver.getLanguages());
                System.out.println("Берём язык: " + receiver.getLanguages().get(0));
                reciverLocale = Locale.of(receiver.getLanguages().get(0));
            }

            newsService.localizeNews(news, reciverLocale);

            NotificationDTO notification = new NotificationDTO(
                messageSource.getMessage("new.news.notification", null, reciverLocale),
                news.getLocalizedTitle(),
                "/news/" + news.getId()
            );
            saveOfflineChatNotification(receiver.getSub(), notification);
        }
    }

    @Override
    public void markAsRead(Notification notification) {
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public FullNotificationDTO toDTO(Notification notification) {
        
        return new FullNotificationDTO(notification.getId(), 
                                       notification.getRecipient().getId(), 
                                       notification.isRead(), 
                                       notification.getTitle(),
                                       notification.getContent(), 
                                       notification.getLink(), 
                                       notification.getType().toString(), 
                                       notification.getImportance().toString(), 
                                       notification.getCreatedAt());
    }
}
