package org.example.backend.listener;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.response.NotificationResponse;
import org.example.backend.entity.*;
import org.example.backend.event.NotificationEvent;
import org.example.backend.repository.*;
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
    private final IUserRepository userRepository;
    private final IProjectRepository projectRepository;
    private final ITeamRepository teamRepository;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        Notification notification = Notification.builder()
                .recipientId(event.getRecipientId())
                .senderId(event.getSenderId())
                .projectId(event.getProjectId())
                .teamId(event.getTeamId())
                .message(event.getMessage())
                .type(event.getType())
                .status(event.getStatus())
                .isRead(false)
                .build();
        
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);

        // Build full DTO so frontend has senderName, teamName, projectName
        String senderName = "Hệ thống";
        String senderAvatar = null;
        if (saved.getSenderId() != null) {
            User sender = userRepository.findById(saved.getSenderId()).orElse(null);
            if (sender != null) {
                senderName = sender.getFullName();
                senderAvatar = sender.getAvatar();
            }
        }

        String projectName = null;
        if (saved.getProjectId() != null) {
            Project project = projectRepository.findById(saved.getProjectId()).orElse(null);
            if (project != null) projectName = project.getName();
        }

        String teamName = null;
        if (saved.getTeamId() != null) {
            Team team = teamRepository.findById(saved.getTeamId()).orElse(null);
            if (team != null) teamName = team.getName();
        }

        NotificationResponse dto = NotificationResponse.fromEntity(saved, senderName, senderAvatar, projectName, teamName);
        
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + event.getRecipientId(), dto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
