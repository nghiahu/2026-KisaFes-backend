package org.example.backend.dto.request;

import lombok.Data;

@Data
public class CompleteSprintRequest {
    /**
     * Sprint ID to move incomplete tasks to.
     * If null → incomplete tasks go to backlog (sprintId = null).
     */
    private String moveToSprintId;
}
