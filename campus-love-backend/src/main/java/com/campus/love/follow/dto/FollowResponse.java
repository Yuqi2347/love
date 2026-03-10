package com.campus.love.follow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowResponse {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isMutual;
    private String remark; // 备注名
}
