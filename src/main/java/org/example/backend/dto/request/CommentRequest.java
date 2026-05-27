package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CommentRequest {
    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
    
    private String parentId;
    
    private List<String> imageUrls;
}
