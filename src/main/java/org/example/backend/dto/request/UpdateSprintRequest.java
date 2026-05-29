package org.example.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateSprintRequest {
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String goal;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
