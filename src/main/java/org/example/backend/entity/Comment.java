package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "comments")
public class Comment extends BaseEntity {
    private String taskId;
    private String userId;
    private String content;
    private String parentId; // ID of parent comment (for threading)
    private List<String> imageUrls; // Send images in comments
    private Map<String, List<String>> reactions; // emoji -> list of userIds
}
