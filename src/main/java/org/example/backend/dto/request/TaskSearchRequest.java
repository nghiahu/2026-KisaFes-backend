package org.example.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskSearchRequest {
    private String projectId;
    private String keyword;
    private String type;
    private String statusId;
    private String assigneeId;
    private String priority;
    
    @Builder.Default
    private int page = 1; // Frontend passes 1-indexed page
    @Builder.Default
    private int size = 10;
}
