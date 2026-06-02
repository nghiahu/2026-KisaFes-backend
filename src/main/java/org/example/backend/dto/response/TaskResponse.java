package org.example.backend.dto.response;

import lombok.Data;
import org.example.backend.entity.Resolution;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskResponse {
    private String id;
    private String taskKey;
    private String projectId;
    private String projectName;
    private String projectCode;
    private String sprintId;
    private String title;
    private String description;
    private String statusId;
    private String statusLabel;
    private String priority;
    private Integer storyPoints;
    private String assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private String reporterId;
    private String reporterName;
    private String reporterAvatar;
    private String type;
    private Resolution resolution;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.List<SubTaskResponse> subTasks;

    // Scrum fields
    private String epicId;
    private Long backlogPosition;
    private Long boardPosition;

    private String teamId;
    private String teamName;

    private List<AttachmentResponse> attachments;

    @Data
    public static class AttachmentResponse {
        private String fileId;
        private String fileName;
        private String fileUrl;
        private String uploadedBy;
        private LocalDateTime uploadedAt;

        public static AttachmentResponse fromEntity(org.example.backend.entity.Task.Attachment entity) {
            if (entity == null) return null;
            AttachmentResponse dto = new AttachmentResponse();
            dto.setFileId(entity.getFileId());
            dto.setFileName(entity.getFileName());
            dto.setFileUrl(entity.getFileUrl());
            dto.setUploadedBy(entity.getUploadedBy());
            dto.setUploadedAt(entity.getUploadedAt());
            return dto;
        }
    }
}
