package org.example.backend.dto.response;

import lombok.*;
import org.example.backend.entity.AuditLog;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLogResponse {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private AuditLog.AuditAction action;
    private String entity;
    private String entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;

    public static AdminAuditLogResponse fromEntity(AuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .userEmail(log.getUserEmail())
                .action(log.getAction())
                .entity(log.getEntity())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }
}
