package org.workswap.core.services;

import org.workswap.core.datasource.main.model.News;
import org.workswap.core.datasource.main.model.Notification;
import org.workswap.core.datasource.main.model.DTOs.FullNotificationDTO;
import org.workswap.core.datasource.main.model.DTOs.NotificationDTO;

public interface NotificationService {

    void saveOfflineChatNotification(String userId, NotificationDTO notification);
    void markAsRead(Notification notification);

    void sendNewsNotification(News news);

    FullNotificationDTO toDTO(Notification notification);
}
