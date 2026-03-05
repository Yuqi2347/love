package com.campus.love.invite.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建等待邀约请求
 */
@Data
public class InviteWaitCreateRequest {

    @NotNull(message = "邀约类型不能为空")
    private List<String> inviteTypes;

    private String periodConfig;

    private String locationPref;

    private Boolean autoAccept;

    @NotNull(message = "有效时长不能为空")
    private Integer expireHours;

    private String remark;
}
