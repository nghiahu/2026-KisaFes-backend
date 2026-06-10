package org.example.backend.dto.request;

import lombok.Data;
import org.example.backend.dto.response.AiGeneratedTaskDto;

import java.util.List;

@Data
public class AiConfirmTasksRequest {
    /** Nếu không null, sẽ tạo 1 Epic mới và gán các tasks vào đó */
    private String epicName;
    private String epicDescription;
    /** Danh sách tasks đã được người dùng xác nhận */
    private List<AiGeneratedTaskDto> tasks;

    private String targetSprintId;
    private String newSprintName;
}
