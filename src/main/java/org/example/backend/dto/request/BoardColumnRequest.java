package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BoardColumnRequest {

    @NotBlank(message = "Column name is required")
    private String name;

    @NotEmpty(message = "Mapped statuses cannot be empty")
    private List<String> mappedStatusIds;

    private int position;
}