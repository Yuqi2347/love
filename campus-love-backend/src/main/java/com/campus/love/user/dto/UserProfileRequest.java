package com.campus.love.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 10, message = "昵称长度需在1-10个字符之间")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @NotBlank(message = "生日不能为空")
    private String birthDate;

    private String birthTime;
    /** V1.1.0：勾选「不知道时辰」时八字权重清零 */
    private Boolean baziUnknown;

    private String school;
    private String major;
    private String grade;
    private String mbti;
    private String bio;
    /** 旧格式：逗号分隔 */
    private String interests;
    /** 新格式 JSON：{"dimension":[{"code":"tag_xxx","sharing":0.5,"intensity":0.5}]} */
    private String interestTags;
    /** 朋友圈展示设置：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
    private String feedVisibility;
}
