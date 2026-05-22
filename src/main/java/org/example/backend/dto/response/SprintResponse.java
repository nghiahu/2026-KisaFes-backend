package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.Sprint;

import java.time.LocalDateTime;

@Data
public class SprintResponse {
    private String id;
    private String projectId;
    private String name;
    private String goal;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    // Metrics được tính toán từ tasks
    private int totalStoryPoints;
    private int completedStoryPoints;
    private int inProgressStoryPoints;
    private int unstartedStoryPoints;
    private int totalTasks;
    private int completedTasks;

    public static SprintResponse fromEntity(Sprint sprint) {
        SprintResponse r = new SprintResponse();
        r.setId(sprint.getId());
        r.setProjectId(sprint.getProjectId());
        r.setName(sprint.getName());
        r.setGoal(sprint.getGoal());
        r.setStatus(sprint.getStatus());
        r.setStartDate(sprint.getStartDate());
        r.setEndDate(sprint.getEndDate());
        r.setCreatedAt(sprint.getCreatedAt());
        return r;
    }
}
