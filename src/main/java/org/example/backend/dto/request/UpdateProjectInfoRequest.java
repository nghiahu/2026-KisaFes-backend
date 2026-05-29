package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProjectInfoRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    private String name;

    private String description;

    @NotBlank(message = "Danh mục không được để trống")
    private String categoryId;
}
