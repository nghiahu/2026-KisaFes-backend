package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectNameRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @Size(min = 1, max = 100, message = "Tên dự án phải từ 1 đến 100 ký tự")
    private String name;
}
