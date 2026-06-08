package org.example.backend.dto.response;

import lombok.*;
import org.example.backend.entity.SystemBroadcast;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastResponse {
    private String id;
    private String title;
    private String message;
    private SystemBroadcast.BroadcastType type;
    private SystemBroadcast.TargetAudience targetAudience;
    private String sentById;
    private String sentByName;
    private LocalDateTime createdAt;

    public static AdminBroadcastResponse fromEntity(SystemBroadcast broadcast) {
        return AdminBroadcastResponse.builder()
                .id(broadcast.getId())
                .title(broadcast.getTitle())
                .message(broadcast.getMessage())
                .type(broadcast.getType())
                .targetAudience(broadcast.getTargetAudience())
                .sentById(broadcast.getSentById())
                .sentByName(broadcast.getSentByName())
                .createdAt(broadcast.getCreatedAt())
                .build();
    }
}
