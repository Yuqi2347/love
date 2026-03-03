package com.campus.love.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserProfileRequest {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @NotBlank(message = "生日不能为空")
    private String birthDate;

    private String school;
    private String major;
    private String grade;
    private String mbti;
    private String bio;
    private String interests;
}
