package com.campus.love.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 破冰灵感返回：先分析聊天记录再给话题建议（借鉴缘分分析格式） */
@Data
@Builder
public class IceBreakTopicsResponse {
    /** 基于聊天记录的分析：关系状态、话题偏好、冷场原因等（约150字） */
    private String analysis;
    /** 3条具体可发的破冰话题 */
    private List<String> topics;
}
