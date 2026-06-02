package org.example.backend.dto.request;

import lombok.Data;

@Data
public class TeamRequest {
    private String name;
    private String description;
    private String avatar;
    private String coverImage;
}
