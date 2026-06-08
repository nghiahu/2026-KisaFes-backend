package org.example.backend.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String userId;
    private String userName;
    private String userEmail;

    private AuditAction action;
    private String entity;      // e.g. "USER", "BLOG", "CATEGORY"
    private String entityId;
    private String details;
    private String ipAddress;

    @CreatedDate
    private LocalDateTime timestamp;

    public enum AuditAction {
        CREATE, UPDATE, DELETE, LOGIN, OTHER
    }
}
