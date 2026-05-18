package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "categories")
public class Category extends BaseEntity {
    private String name;
    private String description;
    
    private List<CategoryStatus> defaultStatuses;
    private List<CategoryBoardColumn> defaultBoardColumns;
    private List<CategoryRole> defaultRoles;

    @Data
    public static class CategoryStatus {
        private String statusId = UUID.randomUUID().toString();
        private String label;
        private StatusCategory category;
        private String color;
    }

    @Data
    public static class CategoryBoardColumn {
        private String name;
        private List<String> mappedStatusIds;
        private int position;
    }

    @Data
    public static class CategoryRole {
        private String id = UUID.randomUUID().toString();
        private String name;
        private Set<String> permissions;
    }
}
