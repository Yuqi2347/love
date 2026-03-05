package com.campus.love.invite.dto;

import lombok.Data;

import java.util.List;

/**
 * 确认参与者请求
 */
@Data
public class InviteConfirmParticipantsRequest {

    private List<Long> userIds;
}
