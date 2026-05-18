package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.StatusCategory;

@Data
public class ProjectStatusResponse {
    private String statusId;
    private String label;
    private String category;
    private String color;
}
