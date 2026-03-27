package com.campus.love.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatCodeRequest {

    @NotBlank(message = "微信登录凭证不能为空")
    private String code;
}
