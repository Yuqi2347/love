package com.campus.love.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 破冰功能状态（技术文档 V1.1.0 第 5.1 节，V26 支持按好友单独设置）
 * 用于聊天框底部「💡 破冰灵感」按钮及「允许对方使用破冰」开关
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceBreakStatusResponse {
    /** 是否显示按钮：互关时 true，未互关时 false */
    private boolean canShow;
    /** 对方是否允许我使用破冰：全局开启或单独授权时 true */
    private boolean targetEnabled;
    /** 我是否已允许对方使用破冰（可单独设置） */
    private boolean allowedByMe;
    /** 是否可设置「允许对方使用破冰」（互关时为 true） */
    private boolean canAllow;
}
