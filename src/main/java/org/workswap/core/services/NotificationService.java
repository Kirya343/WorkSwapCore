package org.workswap.core.services;

import org.workswap.datasource.central.model.News;
import org.workswap.datasource.central.model.Notification;
import org.workswap.common.dto.notification.FullNotificationDTO;
import org.workswap.common.dto.notification.NotificationDTO;

public interface NotificationService {

    void saveOfflineChatNotification(String userId, NotificationDTO notification);
    void markAsRead(Notification notification);

    void sendNewsNotification(News news);

    FullNotificationDTO toDTO(Notification notification);
}
