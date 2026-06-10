package org.example.backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AiSprintPlanResult {
    private String sprintName;
    private String targetSprintId;
    private String sprintGoal;
    private String reasoning;
    private List<AiPlanTaskDto> selectedTasks;
}
