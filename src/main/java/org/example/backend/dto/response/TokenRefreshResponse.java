package org.example.backend.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

    private String accessToken;

    private long expiresIn;
}
