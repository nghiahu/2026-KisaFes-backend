package org.example.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private String id;
    private String taskKey;
    private String projectId;
    private String sprintId;
    private String title;
    private String description;
    private String statusId;
    private String statusLabel;
    private String priority;
    private Integer storyPoints;
    private String assigneeId;
    private String assigneeName;
    private String reporterId;
    private String reporterName;
    private String type; // "story" | "task" | "bug"
    private LocalDateTime createdAt;
}
