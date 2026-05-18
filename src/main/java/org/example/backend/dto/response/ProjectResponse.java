package org.example.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class ProjectResponse {

    private String id;
    private String name;
    private String code;
    private String description;
    private String categoryId;

    private List<ProjectStatusResponse> statuses;
    private List<BoardColumnResponse> boardColumns;
    private List<ProjectRoleResponse> customRoles;

    private Set<String> favoriteBy;

    private String ownerId;
    private List<MemberResponse> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String activeSprintName;
    private int completedTasksCount;
    private int totalTasksCount;
    private int blockedTasksCount;
    private int openIssuesCount;
    private String deadlineDisplay;

    @Data
    public static class MemberResponse {
        private String id;
        private String name;
        private String avatar;
        private String roleName;
    }

    public static ProjectResponse fromEntity(org.example.backend.entity.Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setCode(project.getCode());
        response.setDescription(project.getDescription());
        response.setCategoryId(project.getCategoryId());
        response.setOwnerId(project.getOwnerId());
        response.setFavoriteBy(project.getFavoriteBy());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        if (project.getStatuses() != null) {
            response.setStatuses(project.getStatuses().stream()
                    .map(s -> {
                        ProjectStatusResponse ps = new ProjectStatusResponse();
                        ps.setStatusId(s.getStatusId());
                        ps.setLabel(s.getLabel());
                        ps.setCategory(s.getCategory() != null ? s.getCategory().name() : null);
                        ps.setColor(s.getColor());
                        return ps;
                    }).toList());
        }

        if (project.getBoardColumns() != null) {
            response.setBoardColumns(project.getBoardColumns().stream()
                    .map(c -> {
                        BoardColumnResponse bc = new BoardColumnResponse();
                        bc.setName(c.getName());
                        bc.setMappedStatusIds(c.getMappedStatusIds());
                        bc.setPosition(c.getPosition());
                        return bc;
                    }).toList());
        }

        if (project.getCustomRoles() != null) {
            response.setCustomRoles(project.getCustomRoles().stream()
                    .map(r -> {
                        ProjectRoleResponse pr = new ProjectRoleResponse();
                        pr.setId(r.getId());
                        pr.setName(r.getName());
                        if (r.getPermissions() != null) {
                            pr.setPermissions(r.getPermissions().stream()
                                    .map(Enum::name)
                                    .collect(java.util.stream.Collectors.toSet()));
                        }
                        return pr;
                    }).toList());
        }

        return response;
    }
}
