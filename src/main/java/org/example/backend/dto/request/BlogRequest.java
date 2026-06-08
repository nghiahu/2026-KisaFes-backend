package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.example.backend.entity.Blog;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String excerpt;

    private String content;

    private Blog.BlogStatus status;

    private LocalDateTime publishAt;

    private String thumbnailUrl;

    private List<String> tags;
}
