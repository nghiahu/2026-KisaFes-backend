package org.example.backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AiTaskGenerationResult {
    private String epicName;
    private String epicDescription;
    private List<AiGeneratedTaskDto> tasks;
}
