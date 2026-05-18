package org.example.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;


@Data
public class AddProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Project code is required")
    @Size(min = 2, max = 10, message = "Project code must be between 2 and 10 characters")
    @Pattern(
            regexp = "^[A-Z0-9_-]+$",
            message = "Project code only allows uppercase letters, numbers, underscore and hyphen"
    )
    private String code;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String categoryId;

    @Valid
    private List<ProjectStatusRequest> statuses;

    @Valid
    private List<BoardColumnRequest> boardColumns;

    @Valid
    private List<ProjectRoleRequest> roles;

    private List<String> members;
}