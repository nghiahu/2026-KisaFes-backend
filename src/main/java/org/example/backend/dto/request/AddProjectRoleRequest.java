package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class AddProjectRoleRequest {
    @NotBlank(message = "Tên role không được để trống")
    private String name;
    
    private Set<String> permissions;
}
