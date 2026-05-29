package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.Epic;

import java.time.LocalDateTime;

@Data
public class EpicResponse {
    private String id;
    private String projectId;
    private String name;
    private String description;
    private String color;
    private String createdBy;
    private LocalDateTime createdAt;
    private int taskCount;

    public static EpicResponse fromEntity(Epic epic) {
        EpicResponse r = new EpicResponse();
        r.setId(epic.getId());
        r.setProjectId(epic.getProjectId());
        r.setName(epic.getName());
        r.setDescription(epic.getDescription());
        r.setColor(epic.getColor());
        r.setCreatedBy(epic.getCreatedBy());
        r.setCreatedAt(epic.getCreatedAt());
        return r;
    }
}
