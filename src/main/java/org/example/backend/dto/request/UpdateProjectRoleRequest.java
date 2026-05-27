package org.example.backend.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class UpdateProjectRoleRequest {
    private String name;
    private Set<String> permissions;
}
