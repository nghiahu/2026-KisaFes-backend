package org.example.backend.entity;

import lombok.*;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "blogs")
public class Blog extends BaseEntity {

    @Indexed
    private String title;
    private String excerpt;
    private String content;
    private String authorId;
    @Indexed
    private String authorName;
    private String thumbnailUrl;

    @Builder.Default
    @Indexed
    private BlogStatus status = BlogStatus.DRAFT;

    @Indexed
    private LocalDateTime publishAt;

    @Indexed
    private List<String> tags;

    @Builder.Default
    private long views = 0; 

    public enum BlogStatus {
        DRAFT, PENDING, PUBLISHED, ARCHIVED
    }
}
