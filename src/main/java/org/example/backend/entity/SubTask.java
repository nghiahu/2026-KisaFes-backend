package org.example.backend.entity;

import lombok.Data;
import java.util.UUID;

@Data
public class SubTask {
    private String id = UUID.randomUUID().toString();
    private String title;
    private boolean isDone = false;
}
