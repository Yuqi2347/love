package com.campus.love.chat.constants;

/**
 * 聊天模块常量。
 */
public final class ChatConstants {

    private ChatConstants() {}

    /** 群组状态：活跃 */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** 群聊消息无单独接收人时使用的 receiverId（语义：非单聊） */
    public static final long GROUP_RECEIVER_ID_NONE = 0L;

    /** 消息撤回时间窗口（小时） */
    public static final int RECALL_WINDOW_HOURS = 1;

    /** 聊天历史分页最大每页条数 */
    public static final int MAX_PAGE_SIZE = 100;
}
