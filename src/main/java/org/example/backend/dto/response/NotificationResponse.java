package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.Notification;
import org.example.backend.entity.NotificationStatus;
import org.example.backend.entity.NotificationType;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private String id;
    private String recipientId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String projectId;
    private String projectName;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification notification, String senderName, String senderAvatar, String projectName) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setRecipientId(notification.getRecipientId());
        response.setSenderId(notification.getSenderId());
        response.setSenderName(senderName);
        response.setSenderAvatar(senderAvatar);
        response.setProjectId(notification.getProjectId());
        response.setProjectName(projectName);
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setStatus(notification.getStatus());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
