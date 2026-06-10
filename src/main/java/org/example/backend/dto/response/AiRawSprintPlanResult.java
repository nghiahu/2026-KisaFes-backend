package org.example.backend.dto.response;

import lombok.Data;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRawSprintPlanResult {
    private String sprintName;
    private String targetSprintId;
    private String sprintGoal;
    private String reasoning;
    private List<String> selectedTaskIds;
}
