package com.campus.love.invite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邀约踢人请求（发起人踢出参与者，必须填写至少10字理由）
 */
@Data
public class InviteKickRequest {

    @NotBlank(message = "踢人理由不能为空")
    @Size(min = 10, message = "踢人理由至少10个字")
    private String reason;
}
