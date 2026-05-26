package org.example.backend.dto.response;

import lombok.Data;

@Data
public class SubTaskResponse {
    private String id;
    private String title;
    private boolean isDone;
}
