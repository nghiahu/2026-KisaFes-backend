package org.example.backend.dto.response;

import lombok.*;
import org.example.backend.entity.Blog;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogResponse {
    private String id;
    private String title;
    private String excerpt;
    private String content;
    private String authorId;
    private String authorName;
    private Blog.BlogStatus status;
    private String thumbnailUrl;
    private List<String> tags;
    private long views;
    private LocalDateTime publishAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlogResponse fromEntity(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .authorId(blog.getAuthorId())
                .authorName(blog.getAuthorName())
                .thumbnailUrl(blog.getThumbnailUrl())
                .status(blog.getStatus())
                .tags(blog.getTags())
                .views(blog.getViews())
                .publishAt(blog.getPublishAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}
