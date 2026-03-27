package com.campus.love.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** 是否管理员，用于后台管理端鉴权 */
    @JsonProperty("isAdmin")
    private Boolean isAdmin;
    /** 是否已绑定微信小程序账号 */
    private Boolean wechatBound;
    private String accessToken;
    private String refreshToken;
}
