package org.workswap.core.services;

import org.workswap.core.datasource.central.model.News;
import org.workswap.core.datasource.central.model.Notification;
import org.workswap.core.datasource.central.model.DTOs.FullNotificationDTO;
import org.workswap.core.datasource.central.model.DTOs.NotificationDTO;

public interface NotificationService {

    void saveOfflineChatNotification(String userId, NotificationDTO notification);
    void markAsRead(Notification notification);

    void sendNewsNotification(News news);

    FullNotificationDTO toDTO(Notification notification);
}
