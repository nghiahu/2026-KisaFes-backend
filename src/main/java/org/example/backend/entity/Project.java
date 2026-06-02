package org.example.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Phương pháp quản lý dự án.
 * SCRUM: Sprint-based, có Backlog, Story Points.
 * KANBAN: Continuous flow, không có Sprint.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "projects")
public class Project extends BaseEntity {
    private String name;

    @Indexed(unique = true)
    private String code;
    private String description;
    private String categoryId;

    /** Phương pháp quản lý: SCRUM hoặc KANBAN. Mặc định KANBAN. */
    private Methodology methodology = Methodology.KANBAN;

    public enum Methodology {
        SCRUM, KANBAN
    }

    private List<ProjectStatus> statuses;
    private List<BoardColumn> boardColumns;
    private List<ProjectRole> customRoles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectTeam {
        private String teamId;
        private String roleId;
        private java.time.LocalDateTime assignedAt;
    }
    
    private List<ProjectTeam> teams = new ArrayList<>();

    private Set<String> favoriteBy = new HashSet<>();

    private String ownerId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectRole {
        @Builder.Default
        private String id = java.util.UUID.randomUUID().toString();
        private String name;
        private Set<Permission> permissions;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStatus {
        private String statusId;
        private String label;
        private StatusCategory category;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardColumn {
        private String name;
        private List<String> mappedStatusIds;
        private int position;
    }
}

