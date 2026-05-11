package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "tasks")
public class Task extends BaseEntity {

    @Indexed(unique = true)
    private String taskKey;

    private String projectId;
    private String sprintId;

    private String title;
    private String description;

    private String statusId;
    private String priority;
    private Integer storyPoints;

    private String assigneeId;
    private String reporterId;

    private List<Attachment> attachments;
    private List<TaskRelation> relations;
    private List<SubTask> subTasks;
}

@Data
class Attachment {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String uploadedBy;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}

@Data
class TaskRelation {
    private String targetTaskId;
    private RelationType type;
}

@Data
class SubTask {
    private String id = UUID.randomUUID().toString();
    private String title;
    private boolean isDone = false;
}

