package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "activities")
public class Activity extends BaseEntity {
    private String taskId;
    private String userId;

    private ActivityType type;
    private String content;
    private String parentId;

    private Map<String, Object> metadata;
}

