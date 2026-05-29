package org.example.backend.dto.request;

import lombok.Data;

@Data
public class TaskFilter {
    private String assigneeId;
    private String projectId;
    private String statusId;
    private String priority;
    private String keyword;
    private Boolean overdue;
    private Boolean dueToday;
    
    // Pagination and sorting
    private int page = 1;
    private int size = 20;
    private String sortBy = "updatedAt";
    private String sortDirection = "DESC";
}
