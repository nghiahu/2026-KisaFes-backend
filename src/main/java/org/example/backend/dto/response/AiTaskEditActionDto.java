package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskEditActionDto {
    private String taskId;
    private String taskKey;
    private String fieldToChange; // "statusId", "assigneeId", "dueDate", "priority", "type", "title"
    private String oldValue;
    private String newValue;
    private String oldValueDisplay;
    private String newValueDisplay;
    private String reason;
}
