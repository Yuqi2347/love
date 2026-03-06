package com.campus.love.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 群聊列表项（我加入的群，用于会话/群聊入口）
 */
@Data
@Builder
public class ChatGroupItemResponse {

    private Long groupId;
    private Long inviteId;
    private String name;
    private Integer memberCount;
    private String lastMessage;
    private String lastTime;
    /** 成员头像 URL 列表，用于列表左侧多头像展示（类微信） */
    private List<String> memberAvatarUrls;
}
