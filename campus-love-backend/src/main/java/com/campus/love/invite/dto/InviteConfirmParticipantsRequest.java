package com.campus.love.invite.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 确认参与者请求
 */
@Data
public class InviteConfirmParticipantsRequest {

    /** 要确认的用户 ID 列表，空列表表示仅更新邀约状态为已确认 */
    private List<Long> userIds = Collections.emptyList();
}
