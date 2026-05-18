package org.example.backend.service;

import org.example.backend.dto.response.NotificationResponse;
import java.util.List;

public interface INotificationService {
    List<NotificationResponse> getMyNotifications();
    NotificationResponse acceptInvitation(String id);
    NotificationResponse declineInvitation(String id);
}
