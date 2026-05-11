package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "projects")
public class Project extends BaseEntity {
    private String name;

    @Indexed(unique = true)
    private String code;
    private String description;

    private List<ProjectStatus> statuses;
    private List<BoardColumn> boardColumns;

    private String ownerId;
}

@Data
class ProjectStatus {
    private String statusId;
    private String label;
    private StatusCategory category;
    private String color;
}

@Data
class BoardColumn {
    private String name;
    private List<String> mappedStatusIds;
    private int position;
}
