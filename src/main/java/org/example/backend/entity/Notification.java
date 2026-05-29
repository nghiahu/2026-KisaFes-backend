package org.example.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notifications")
public class Notification extends BaseEntity {
    private String recipientId; // Người nhận thông báo
    private String senderId;    // Người gửi lời mời (Chủ dự án)
    private String projectId;   // ID của dự án được mời
    private String message;     // Nội dung thông điệp
    private NotificationType type;
    private NotificationStatus status;
    private boolean isRead;
}
