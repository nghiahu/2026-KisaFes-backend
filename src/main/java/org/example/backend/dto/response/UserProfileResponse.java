package org.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String email;
    private String fullName;
    private String userName;
    private String avatar;
    private String bio;
    private boolean isPublic;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userName(user.getUserName())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .isPublic(user.isPublic())
                .build();
    }
}
