package org.example.backend.dto.response;

import lombok.*;
import org.example.backend.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private String id;
    private String fullName;
    private String email;
    private String userName;
    private String avatar;
    private Set<String> roles;
    private boolean active;
    private LocalDateTime createdAt;

    public static AdminUserResponse fromEntity(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userName(user.getUserName())
                .avatar(user.getAvatar())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream()
                              .map(r -> r.getName())
                              .collect(Collectors.toSet())
                        : Set.of())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
