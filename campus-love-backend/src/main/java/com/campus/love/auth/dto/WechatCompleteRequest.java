package com.campus.love.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WechatCompleteRequest {

    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String verifyCode;

    /** 邮箱未注册时必填 */
    private String nickname;

    /** 邮箱未注册时必填（6-32） */
    private String password;
}
