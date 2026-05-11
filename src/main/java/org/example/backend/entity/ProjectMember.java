package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "project_members")
public class ProjectMember extends BaseEntity {

    private String projectId;
    private String userId;

    private String roleId;

    /** Thời điểm user được thêm vào project (có thể set trong service khi gán member). */
    private LocalDateTime joinedAt;
}