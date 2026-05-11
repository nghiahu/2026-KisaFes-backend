package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "sprints")
public class Sprint extends BaseEntity {
    private String projectId;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String goal;
    private String status;
}
