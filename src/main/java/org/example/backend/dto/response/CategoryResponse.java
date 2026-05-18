package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.StatusCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private List<CategoryStatusResponse> defaultStatuses;
    private List<CategoryBoardColumnResponse> defaultBoardColumns;
    private List<CategoryRoleResponse> defaultRoles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse fromEntity(org.example.backend.entity.Category category) {
        if (category == null) return null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .defaultStatuses(category.getDefaultStatuses() != null ?
                        category.getDefaultStatuses().stream().map(CategoryStatusResponse::fromEntity).toList() : null)
                .defaultBoardColumns(category.getDefaultBoardColumns() != null ?
                        category.getDefaultBoardColumns().stream().map(CategoryBoardColumnResponse::fromEntity).toList() : null)
                .defaultRoles(category.getDefaultRoles() != null ?
                        category.getDefaultRoles().stream().map(CategoryRoleResponse::fromEntity).toList() : null)
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStatusResponse {
        private String statusId;
        private String label;
        private StatusCategory category;
        private String color;

        public static CategoryStatusResponse fromEntity(org.example.backend.entity.Category.CategoryStatus status) {
            if (status == null) return null;
            return CategoryStatusResponse.builder()
                    .statusId(status.getStatusId())
                    .label(status.getLabel())
                    .category(status.getCategory())
                    .color(status.getColor())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryBoardColumnResponse {
        private String name;
        private List<String> mappedStatusIds;
        private int position;

        public static CategoryBoardColumnResponse fromEntity(org.example.backend.entity.Category.CategoryBoardColumn column) {
            if (column == null) return null;
            return CategoryBoardColumnResponse.builder()
                    .name(column.getName())
                    .mappedStatusIds(column.getMappedStatusIds())
                    .position(column.getPosition())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryRoleResponse {
        private String id;
        private String name;
        private Set<String> permissions;

        public static CategoryRoleResponse fromEntity(org.example.backend.entity.Category.CategoryRole role) {
            if (role == null) return null;
            return CategoryRoleResponse.builder()
                    .id(role.getId())
                    .name(role.getName())
                    .permissions(role.getPermissions())
                    .build();
        }
    }
}
