package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
}
