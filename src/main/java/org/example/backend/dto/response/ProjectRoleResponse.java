package org.example.backend.dto.response;

import lombok.Data;
import java.util.Set;

@Data
public class ProjectRoleResponse {
    private String id;
    private String name;
    private Set<String> permissions;
}
