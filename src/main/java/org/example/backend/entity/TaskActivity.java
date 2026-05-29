package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persistent audit log for important task state changes.
 * Used by burndown chart (historical remaining points) and sprint reports.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "task_activities")
public class TaskActivity extends BaseEntity {
    private String taskId;
    private String projectId;
    private String sprintId;   // snapshot of sprint at time of action
    private String userId;

    /** Field that changed, e.g. "statusId", "sprintId", "storyPoints" */
    private String field;
    private String oldValue;
    private String newValue;

    private TaskActivityType actionType;

    public enum TaskActivityType {
        CREATE_TASK,
        UPDATE_STATUS,
        MOVE_SPRINT,
        UPDATE_ASSIGNEE,
        UPDATE_PRIORITY,
        UPDATE_STORY_POINTS,
        DELETE_TASK
    }
}
