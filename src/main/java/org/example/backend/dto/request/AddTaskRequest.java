package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTaskRequest {
    @NotBlank(message = "Project ID không được để trống")
    private String projectId;
    
    private String sprintId;
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String description;
    private String statusId;
    private String priority = "Medium";
    private Integer storyPoints = 0;
    private String assigneeId;
    private String type = "task"; // "story" | "task" | "bug"
    private java.time.LocalDateTime dueDate;
    private String teamId;
}
