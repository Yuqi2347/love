package com.campus.love.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private Integer gender;
    private String birthDate;
    private String birthTime;
    /** 年龄（仅他人资料返回，本人返回 null 使用 birthDate） */
    private Integer age;
    private String school;
    private String major;
    private String grade;
    private Integer activityScore;
    private Integer userLevel;
    private Boolean isAdmin;
    // 邀约相关：信用分与统计
    private Integer creditScore;
    private Integer inviteCount;
    private Integer participateCount;
    private String mbti;
    private String zodiac;
    private String bazi;
    /** V1.1.0：不知道时辰时八字权重清零 */
    private Boolean baziUnknown;
    private String avatarUrl;
    /** 个人主页背景图 URL */
    private String coverImageUrl;
    private String bio;
    /** 旧格式（兼容），新格式用 interestTags */
    private String interests;
    /** 新格式 JSON 字符串 */
    private String interestTags;
    private Boolean profileComplete;
    /** 朋友圈展示设置：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
    private String feedVisibility;
    /** 动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部 */
    private Integer feedVisibilityTime;
    /** V24：是否开启破冰功能（仅本人） */
    private Boolean iceBreakEnabled;
    /** V24：AI 信息公开授权设置 JSON（仅本人） */
    private String aiDisclosureSettings;
}
