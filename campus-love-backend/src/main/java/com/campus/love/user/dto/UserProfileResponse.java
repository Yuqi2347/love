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
    private String avatarUrl;
    /** 个人主页背景图 URL */
    private String coverImageUrl;
    private String bio;
    private String interests;
    private Boolean profileComplete;
    /** 朋友圈展示设置：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
    private String feedVisibility;
    /** 动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部 */
    private Integer feedVisibilityTime;
}
