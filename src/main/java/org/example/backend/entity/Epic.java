package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Epic – a large body of work that can be broken down into stories/tasks.
 * Epics are labels/grouping metadata; they do NOT appear as columns on the board.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "epics")
public class Epic extends BaseEntity {
    private String projectId;
    private String name;
    private String description;
    /** HEX color for badge, e.g. "#6366f1" */
    private String color = "#6366f1";
    private String createdBy;
}
