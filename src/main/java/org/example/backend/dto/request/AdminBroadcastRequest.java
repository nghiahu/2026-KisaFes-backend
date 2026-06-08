package org.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.backend.entity.SystemBroadcast;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String message;

    @NotNull(message = "Loại thông báo không được để trống")
    private SystemBroadcast.BroadcastType type;

    @NotNull(message = "Đối tượng nhận không được để trống")
    private SystemBroadcast.TargetAudience targetAudience;
}
