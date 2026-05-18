package org.example.backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class BoardColumnResponse {
    private String name;
    private List<String> mappedStatusIds;
    private int position;
}
