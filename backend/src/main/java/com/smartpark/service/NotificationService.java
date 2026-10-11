package com.smartpark.service;

import com.smartpark.entity.Notification;
import com.smartpark.entity.User;
import com.smartpark.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Notification sendNotification(User user, String title, String message, NotificationType type);

    List<Notification> getRecentNotifications(Long userId);

    Page<Notification> getNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
