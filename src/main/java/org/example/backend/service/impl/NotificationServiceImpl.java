package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.response.NotificationResponse;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.INotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final INotificationRepository notificationRepository;
    private final IProjectRepository projectRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final IUserRepository userRepository;
    private final ITeamRepository teamRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void broadcastProjectUpdate(String teamId) {
        try {
            java.util.List<Project> projects = projectRepository.findProjectsByTeamIds(java.util.Collections.singletonList(teamId));
            for (Project p : projects) {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", "UPDATE_PROJECT");
                payload.put("data", null);
                messagingTemplate.convertAndSend("/topic/project/" + p.getId(), (Object) payload);
            }
            
            // Also broadcast UPDATE_TEAM so TeamDetail.tsx can listen to it
            java.util.Map<String, Object> teamPayload = new java.util.HashMap<>();
            teamPayload.put("type", "UPDATE_TEAM");
            teamPayload.put("data", null);
            messagingTemplate.convertAndSend("/topic/team/" + teamId, (Object) teamPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId);

        return notifications.stream().map(n -> {
            String senderName = "Hệ thống";
            String senderAvatar = null;
            if (n.getSenderId() != null) {
                User sender = userRepository.findById(n.getSenderId()).orElse(null);
                if (sender != null) {
                    senderName = sender.getFullName();
                    senderAvatar = sender.getAvatar();
                }
            }

            String projectName = null;
            if (n.getProjectId() != null) {
                Project project = projectRepository.findById(n.getProjectId()).orElse(null);
                if (project != null) {
                    projectName = project.getName();
                }
            }

            String teamName = null;
            if (n.getTeamId() != null) {
                Team team = teamRepository.findById(n.getTeamId()).orElse(null);
                if (team != null) {
                    teamName = team.getName();
                }
            }

            return NotificationResponse.fromEntity(n, senderName, senderAvatar, projectName, teamName);
        }).toList();
    }

    @Override
    @Transactional
    public NotificationResponse acceptInvitation(String id) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lời mời"));

        if (!notification.getRecipientId().equals(currentUserId)) {
            throw new CustomBusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
        }

        if (notification.getType() != NotificationType.INVITATION || notification.getStatus() != NotificationStatus.PENDING) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Lời mời không hợp lệ hoặc đã được xử lý");
        }

        String resolvedProjectName = null;
        String resolvedTeamName = null;

        if (notification.getProjectId() != null) {
            Project project = projectRepository.findById(notification.getProjectId())
                    .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án tương ứng"));
            resolvedProjectName = project.getName();

            // Kiểm tra xem user đã là member chưa
            boolean alreadyMember = projectMemberRepository.findByProjectId(project.getId()).stream()
                    .anyMatch(m -> m.getUserId().equals(currentUserId));

            if (!alreadyMember) {
                // Tạo ProjectMember mới
                ProjectMember member = new ProjectMember();
                member.setProjectId(project.getId());
                member.setUserId(currentUserId);
                member.setJoinedAt(LocalDateTime.now());

                // Gán role mặc định (developer/member) nếu có
                if (project.getCustomRoles() != null) {
                    project.getCustomRoles().stream()
                            .filter(r -> r.getName().toLowerCase().contains("developer") || r.getName().toLowerCase().contains("member"))
                            .findFirst()
                            .ifPresent(r -> member.setRoleId(r.getId()));
                }

                projectMemberRepository.save(member);
            }
        } else if (notification.getTeamId() != null) {
            Team team = teamRepository.findById(notification.getTeamId())
                    .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhóm tương ứng"));
            resolvedTeamName = team.getName();

            boolean alreadyMember = teamMemberRepository.findByTeamId(team.getId()).stream()
                    .anyMatch(m -> m.getUserId().equals(currentUserId));
            
            if (!alreadyMember) {
                TeamMember member = new TeamMember();
                member.setTeamId(team.getId());
                member.setUserId(currentUserId);
                member.setRole("MEMBER");
                member.setJoinedAt(LocalDateTime.now());
                teamMemberRepository.save(member);
                
                broadcastProjectUpdate(team.getId());
            }
        } else {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Lời mời không hợp lệ (không chứa projectId hoặc teamId)");
        }

        // Cập nhật trạng thái thông báo thành ACCEPTED
        notification.setStatus(NotificationStatus.ACCEPTED);
        Notification savedNotification = notificationRepository.save(notification);

        // Lấy thông tin hiển thị
        User sender = userRepository.findById(notification.getSenderId()).orElse(null);
        String senderName = sender != null ? sender.getFullName() : "Hệ thống";
        String senderAvatar = sender != null ? sender.getAvatar() : null;

        return NotificationResponse.fromEntity(savedNotification, senderName, senderAvatar, resolvedProjectName, resolvedTeamName);
    }

    @Override
    @Transactional
    public NotificationResponse declineInvitation(String id) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lời mời"));

        if (!notification.getRecipientId().equals(currentUserId)) {
            throw new CustomBusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
        }

        if (notification.getType() != NotificationType.INVITATION || notification.getStatus() != NotificationStatus.PENDING) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Lời mời không hợp lệ hoặc đã được xử lý");
        }

        // Cập nhật trạng thái thông báo thành DECLINED
        notification.setStatus(NotificationStatus.DECLINED);
        Notification savedNotification = notificationRepository.save(notification);

        String projectName = null;
        if (notification.getProjectId() != null) {
            Project project = projectRepository.findById(notification.getProjectId()).orElse(null);
            projectName = project != null ? project.getName() : null;
        }
        
        String teamName = null;
        if (notification.getTeamId() != null) {
            Team team = teamRepository.findById(notification.getTeamId()).orElse(null);
            teamName = team != null ? team.getName() : null;
        }

        User sender = userRepository.findById(notification.getSenderId()).orElse(null);
        String senderName = sender != null ? sender.getFullName() : "Hệ thống";
        String senderAvatar = sender != null ? sender.getAvatar() : null;

        return NotificationResponse.fromEntity(savedNotification, senderName, senderAvatar, projectName, teamName);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String id) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông báo"));

        if (!notification.getRecipientId().equals(currentUserId)) {
            throw new CustomBusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
        }

        Notification savedNotification = notificationRepository.save(notification);

        Project project = null;
        if (notification.getProjectId() != null) {
            project = projectRepository.findById(notification.getProjectId()).orElse(null);
        }
        String projectName = project != null ? project.getName() : null;
        String teamName = null;
        if (notification.getTeamId() != null) {
            Team team = teamRepository.findById(notification.getTeamId()).orElse(null);
            teamName = team != null ? team.getName() : null;
        }

        User sender = null;
        if (notification.getSenderId() != null) {
            sender = userRepository.findById(notification.getSenderId()).orElse(null);
        }
        String senderName = sender != null ? sender.getFullName() : "Hệ thống";
        String senderAvatar = sender != null ? sender.getAvatar() : null;

        return NotificationResponse.fromEntity(savedNotification, senderName, senderAvatar, projectName, teamName);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserId = userDetails.getUserId();

        List<Notification> unreadNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId)
                .stream().filter(n -> !n.isRead()).toList();

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        
        if (!unreadNotifications.isEmpty()) {
            notificationRepository.saveAll(unreadNotifications);
        }
    }
}
