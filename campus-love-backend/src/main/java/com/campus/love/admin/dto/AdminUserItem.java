package com.campus.love.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项（不含密码）
 */
@Data
@Builder
public class AdminUserItem {

    private Long id;
    private String email;
    private String nickname;
    private String school;
    private Integer status;
    private Boolean isAdmin;
    private Integer creditScore;
    private Integer userLevel;
    private Integer inviteCount;
    private Integer participateCount;
    private LocalDateTime createdAt;
}
