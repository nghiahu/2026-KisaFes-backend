package org.example.backend.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.example.backend.entity.SystemPermission;

import java.util.Set;

public class AddRoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;

    @NotEmpty(message = "Permissions cannot be empty")
    private Set<SystemPermission> permissions;
}
