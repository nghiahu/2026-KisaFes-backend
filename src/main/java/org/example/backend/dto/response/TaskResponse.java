package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.Resolution;
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
    private String type;
    private Resolution resolution;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
