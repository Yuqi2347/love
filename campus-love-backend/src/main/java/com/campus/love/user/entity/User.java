package com.campus.love.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_user")
public class User {

    /** 新用户为 8 位数字随机 id；历史数据可能为较小自增值 */
    @TableId(type = IdType.INPUT)
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
    /** 个人主页背景图 URL */
    private String coverImageUrl;
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

    /** 上次查看粉丝列表时间（用于新粉丝红点消除） */
    private LocalDateTime lastFollowerViewedAt;
    /** 上次查看朋友圈点赞/评论活动时间 */
    private LocalDateTime lastFeedActivityViewedAt;

    /** 心动时刻：照片URL（可选） */
    private String momentPhotoUrl;
    /** 心动时刻：自评颜值分（1-10） */
    private Integer momentSelfScore;
    /** 心动时刻：是否被禁止参加 */
    @TableField("moment_banned")
    private Boolean momentBanned;
    /** 心动时刻：连续未匹配优先权计数 */
    private Integer momentPriorityCount;
    /** 上次查看邀约活动时间（我的邀约有人加入/发言、等待邀约匹配成功） */
    private LocalDateTime lastInviteActivityViewedAt;

    /** 朋友圈展示设置：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
    private String feedVisibility;
    /** 动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部 */
    private Integer feedVisibilityTime;

    /** V24：生辰时辰是否不知道（八字权重清零） */
    @Getter(AccessLevel.NONE)
    @TableField("bazi_unknown")
    private Boolean baziUnknown;
    /** V24：是否开启破冰功能 */
    @TableField("ice_break_enabled")
    private Boolean iceBreakEnabled;
    /** V24：AI信息公开授权设置 JSON */
    private String aiDisclosureSettings;

    /** V30：软删除，NULL=正常，有值=已注销 */
    private LocalDateTime deletedAt;
    /** V30：注销原因枚举 */
    private Integer deleteReason;

    /** AI 头像工作室已用免费次数（成功生成后递增） */
    @TableField("avatar_studio_used_count")
    private Integer avatarStudioUsedCount;

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

    public int getMomentPriorityCountOrDefault() {
        return momentPriorityCount != null ? momentPriorityCount : 0;
    }

    /** 任一方 bazi_unknown=true 时八字不参与计算 */
    public Boolean getBaziUnknown() {
        return baziUnknown;
    }

}
