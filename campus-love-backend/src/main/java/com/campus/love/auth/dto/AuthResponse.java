package com.campus.love.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Boolean profileComplete;
    private String accessToken;
    private String refreshToken;
}
