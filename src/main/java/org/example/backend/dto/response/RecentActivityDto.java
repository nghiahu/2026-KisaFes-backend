package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.TaskActivity;

import java.time.LocalDateTime;

@Data
public class RecentActivityDto {
    private String id;
    
    // User info
    private String userId;
    private String userName;
    private String userAvatar;
    
    // Task info
    private String taskId;
    private String taskKey;
    private String taskTitle;
    private String taskType;
    private String taskStatusId;
    
    // Activity info
    private TaskActivity.TaskActivityType actionType;
    private String field;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;
}
