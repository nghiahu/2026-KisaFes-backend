package org.example.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CommentResponse {
    private String id;
    private String taskId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String parentId;
    private List<String> imageUrls;
    private Map<String, List<String>> reactions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
