package org.example.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.Category;
import org.example.backend.entity.StatusCategory;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    private String description;

    @Valid
    private List<CategoryStatusRequest> defaultStatuses;
    @Valid
    private List<CategoryBoardColumnRequest> defaultBoardColumns;
    @Valid
    private List<CategoryRoleRequest> defaultRoles;

    public Category toEntity() {
        Category category = new Category();
        category.setName(this.name);
        category.setDescription(this.description);
        
        if (this.defaultStatuses != null) {
            category.setDefaultStatuses(this.defaultStatuses.stream()
                    .map(CategoryStatusRequest::toEntity)
                    .toList());
        }
        
        if (this.defaultBoardColumns != null) {
            category.setDefaultBoardColumns(this.defaultBoardColumns.stream()
                    .map(CategoryBoardColumnRequest::toEntity)
                    .toList());
        }
        
        if (this.defaultRoles != null) {
            category.setDefaultRoles(this.defaultRoles.stream()
                    .map(CategoryRoleRequest::toEntity)
                    .toList());
        }
        
        return category;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStatusRequest {
        private String statusId;
        @NotBlank(message = "Status label is required")
        private String label;
        private StatusCategory category;
        private String color;

        public Category.CategoryStatus toEntity() {
            Category.CategoryStatus status = new Category.CategoryStatus();
            if (this.statusId != null) status.setStatusId(this.statusId);
            status.setLabel(this.label);
            status.setCategory(this.category);
            status.setColor(this.color);
            return status;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryBoardColumnRequest {
        @NotBlank(message = "Column name is required")
        private String name;
        private List<String> mappedStatusIds;
        private int position;

        public Category.CategoryBoardColumn toEntity() {
            Category.CategoryBoardColumn column = new Category.CategoryBoardColumn();
            column.setName(this.name);
            column.setMappedStatusIds(this.mappedStatusIds);
            column.setPosition(this.position);
            return column;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryRoleRequest {
        private String id;
        @NotBlank(message = "Role name is required")
        private String name;
        private Set<String> permissions;

        public Category.CategoryRole toEntity() {
            Category.CategoryRole role = new Category.CategoryRole();
            if (this.id != null) role.setId(this.id);
            role.setName(this.name);
            role.setPermissions(this.permissions);
            return role;
        }
    }
}
