package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddSubTaskRequest {
    @NotBlank(message = "Title is required")
    private String title;
}
