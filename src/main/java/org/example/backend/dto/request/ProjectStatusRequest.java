package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.example.backend.entity.StatusCategory;

@Data
public class ProjectStatusRequest {

    @NotBlank(message = "Status ID is required")
    private String statusId;

    @NotBlank(message = "Status label is required")
    private String label;

    private StatusCategory category;

    private String color;
}