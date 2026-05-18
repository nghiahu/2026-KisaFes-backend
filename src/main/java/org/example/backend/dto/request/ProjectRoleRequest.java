package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ProjectRoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;
    private Set<String> permissions;
}
