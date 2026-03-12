package com.campus.love.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolItem {
    private String name;
    private String domain;
    /** 邮箱后缀正则，用于注册时校验（如 szu.edu.cn 表示邮箱须以 @szu.edu.cn 结尾） */
    private String emailSuffix;
}
