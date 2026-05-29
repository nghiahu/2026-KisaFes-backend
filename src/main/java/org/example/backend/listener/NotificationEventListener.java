package org.example.backend.listener;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Notification;
import org.example.backend.event.NotificationEvent;
import org.example.backend.repository.INotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final INotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        Notification notification = Notification.builder()
                .recipientId(event.getRecipientId())
                .senderId(event.getSenderId())
                .projectId(event.getProjectId())
                .message(event.getMessage())
                .type(event.getType())
                .status(event.getStatus())
                .isRead(false)
                .build();
        
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + event.getRecipientId(), saved);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
