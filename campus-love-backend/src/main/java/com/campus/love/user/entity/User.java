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
    private Boolean isAdmin;
    private String mbti;
    private String zodiac;
    private String bazi;
    private String avatarUrl;
    private String bio;
    private String interests;
    private Boolean profileComplete;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
