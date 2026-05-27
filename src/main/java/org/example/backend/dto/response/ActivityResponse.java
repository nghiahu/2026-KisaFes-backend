package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.ActivityType;
import java.time.LocalDateTime;

@Data
public class ActivityResponse {
    private String id;
    private String taskId;
    private String userId;
    private String userName;
    private String userAvatar;
    private ActivityType type;
    private String content;
    private String parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
