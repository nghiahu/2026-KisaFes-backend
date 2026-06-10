package org.example.backend.dto.response;

import lombok.Data;

@Data
public class AiPlanTaskDto {
    private String id;
    private String taskKey;
    private String title;
    private String type;
    private String priority;
}
