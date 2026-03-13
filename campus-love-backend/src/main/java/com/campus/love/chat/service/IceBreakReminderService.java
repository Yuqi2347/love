package com.campus.love.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.chat.entity.IceBreakReminderLog;
import com.campus.love.chat.mapper.IceBreakReminderLogMapper;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 破冰功能提醒服务（技术文档 V1.1.0 第 5.2 节）
 * 聊天达到20条且有一方未开启破冰时，最多提醒2次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IceBreakReminderService {

    private static final int MSG_THRESHOLD = 20;
    private static final int MAX_REMINDER_COUNT = 2;
    private static final String REMINDER_TYPE = "ICE_BREAK";

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final IceBreakReminderLogMapper reminderLogMapper;

    /**
     * 检查是否应展示破冰提醒
     * @return true 表示应展示提醒条
     */
    public boolean shouldRemind(Long selfId, Long targetId) {
        User self = userMapper.selectById(selfId);
        User target = userMapper.selectById(targetId);
        if (self == null || target == null) return false;

        Boolean selfEnabled = Boolean.TRUE.equals(self.getIceBreakEnabled());
        Boolean targetEnabled = Boolean.TRUE.equals(target.getIceBreakEnabled());
        if (selfEnabled && targetEnabled) return false;

        int msgCount = countMessages(selfId, targetId);
        if (msgCount < MSG_THRESHOLD) return false;

        int reminderCount = countReminders(selfId, targetId);
        return reminderCount < MAX_REMINDER_COUNT;
    }

    /**
     * 记录已提醒，调用方在展示提醒条后调用
     */
    public void recordReminder(Long fromUserId, Long toUserId) {
        try {
            IceBreakReminderLog entry = new IceBreakReminderLog();
            entry.setFromUserId(fromUserId);
            entry.setToUserId(toUserId);
            entry.setReminderType(REMINDER_TYPE);
            reminderLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("Record ice break reminder failed (may duplicate): {}", e.getMessage());
        }
    }

    private int countMessages(Long userId1, Long userId2) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<com.campus.love.chat.entity.Message>()
                        .and(w -> w.nested(n -> n.eq(com.campus.love.chat.entity.Message::getSenderId, userId1).eq(com.campus.love.chat.entity.Message::getReceiverId, userId2))
                                .or(o -> o.nested(n -> n.eq(com.campus.love.chat.entity.Message::getSenderId, userId2).eq(com.campus.love.chat.entity.Message::getReceiverId, userId1))))
                        .isNull(com.campus.love.chat.entity.Message::getGroupId)
                        .and(w -> w.isNull(com.campus.love.chat.entity.Message::getDeleted).or().eq(com.campus.love.chat.entity.Message::getDeleted, 0))
        );
        return count != null ? count.intValue() : 0;
    }

    private int countReminders(Long fromUserId, Long toUserId) {
        Long c = reminderLogMapper.selectCount(
                new LambdaQueryWrapper<IceBreakReminderLog>()
                        .eq(IceBreakReminderLog::getFromUserId, fromUserId)
                        .eq(IceBreakReminderLog::getToUserId, toUserId)
                        .eq(IceBreakReminderLog::getReminderType, REMINDER_TYPE)
        );
        return c != null ? c.intValue() : 0;
    }
}
