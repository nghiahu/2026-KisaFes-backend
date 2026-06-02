package org.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TeamResponse {
    private String id;
    private String name;
    private String description;
    private String avatar;
    private String coverImage;
    private List<MemberInfo> members;

    @Data
    @Builder
    public static class MemberInfo {
        private String id;
        private String name;
        private String email;
        private String avatar;
        private String role;
        private String joinedAt;
    }
}
