package org.example.backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AiConfirmSprintPlanRequest {
    private String sprintName;
    private String targetSprintId;
    private String sprintGoal;
    private List<String> taskIds; // The IDs of tasks to move into this new sprint
}
