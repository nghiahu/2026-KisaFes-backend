package org.example.backend.dto.request;

import lombok.Data;

@Data
public class MoveTaskSprintRequest {
    /** Target sprint ID. Set to null to move task to backlog. */
    private String sprintId;
}
