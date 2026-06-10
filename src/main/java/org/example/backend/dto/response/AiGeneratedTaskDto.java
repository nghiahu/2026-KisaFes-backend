package org.example.backend.dto.response;

import lombok.Data;

@Data
public class AiGeneratedTaskDto {
    private String title;
    private String description;
    private String type;        // "task" | "story" | "bug"
    private String priority;    // "Lowest" | "Low" | "Medium" | "High" | "Highest"
    private Integer storyPoints;
    private String suggestedAssigneeId;
    private String suggestedAssigneeName;
    private String dueDate; // yyyy-MM-dd
}
