package org.example.backend.event;

import lombok.Getter;
import org.example.backend.entity.NotificationStatus;
import org.example.backend.entity.NotificationType;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    
    private final String recipientId;
    private final String senderId;
    private final String projectId;
    private final String teamId;
    private final String message;
    private final NotificationType type;
    private final NotificationStatus status;

    public NotificationEvent(Object source, String recipientId, String senderId, String projectId, String message, NotificationType type, NotificationStatus status) {
        super(source);
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.projectId = projectId;
        this.teamId = null;
        this.message = message;
        this.type = type;
        this.status = status;
    }

    public NotificationEvent(Object source, String recipientId, String senderId, String projectId, String teamId, String message, NotificationType type, NotificationStatus status) {
        super(source);
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.projectId = projectId;
        this.teamId = teamId;
        this.message = message;
        this.type = type;
        this.status = status;
    }
}
