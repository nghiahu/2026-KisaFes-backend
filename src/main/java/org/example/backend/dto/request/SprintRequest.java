package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SprintRequest {

    @NotBlank(message = "Sprint name is required")
    @Size(max = 100, message = "Sprint name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Sprint goal cannot exceed 500 characters")
    private String goal;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
