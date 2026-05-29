package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EpicRequest {
    @NotBlank(message = "Tên epic không được để trống")
    @Size(max = 120, message = "Tên epic không quá 120 ký tự")
    private String name;

    @Size(max = 500)
    private String description;

    /** HEX color, e.g. "#6366f1". Default applied server-side if null. */
    private String color;
}
