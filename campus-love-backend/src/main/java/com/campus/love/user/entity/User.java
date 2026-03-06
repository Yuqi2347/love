package com.campus.love.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;
    private String password;
    private String nickname;
    private Integer gender;
    private LocalDate birthDate;
    private LocalTime birthTime;
    private String school;
    private String major;
    private String grade;
    private Integer activityScore;
    private Integer userLevel;
    @TableField("is_admin")
    private Boolean isAdmin;
    private String mbti;
    private String zodiac;
    private String bazi;
    private String avatarUrl;
    private String bio;
    private String interests;
    private Boolean profileComplete;
    private Integer status;

    /** 邀约模块：发起邀约次数 */
    private Integer inviteCount;
    /** 邀约模块：参与邀约次数 */
    private Integer participateCount;
    /** 邀约模块：信用分，默认 100 */
    private Integer creditScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 信用分默认值 100，用于邀约等模块 */
    public static final int DEFAULT_CREDIT_SCORE = 100;

    public int getCreditScoreOrDefault() {
        return creditScore != null ? creditScore : DEFAULT_CREDIT_SCORE;
    }

    public int getParticipantCountOrDefault() {
        return participateCount != null ? participateCount : 0;
    }

    public int getInviteCountOrDefault() {
        return inviteCount != null ? inviteCount : 0;
    }
}
