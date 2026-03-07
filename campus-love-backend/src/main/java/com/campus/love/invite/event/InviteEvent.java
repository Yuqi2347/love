package com.campus.love.invite.event;

import com.campus.love.invite.entity.Invite;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.List;

/**
 * 邀约领域事件：状态变更时发布，由监听器统一处理站内通知等。
 */
@Getter
public class InviteEvent extends ApplicationEvent {

    private final InviteEventType type;
    private final Invite invite;
    /** 操作人（加入/退出/取消的当前用户） */
    private final Long actorUserId;
    /** 通知目标用户 ID 列表（如取消时所有参与者） */
    private final List<Long> targetUserIds;
    private final String reason;

    public InviteEvent(Object source, InviteEventType type, Invite invite, Long actorUserId,
                      List<Long> targetUserIds, String reason) {
        super(source);
        this.type = type;
        this.invite = invite;
        this.actorUserId = actorUserId;
        this.targetUserIds = targetUserIds != null ? targetUserIds : Collections.emptyList();
        this.reason = reason;
    }

    public static InviteEvent joinSuccess(Invite invite, Long participantUserId) {
        return new InviteEvent(invite, InviteEventType.JOIN_SUCCESS, invite, participantUserId, null, null);
    }

    public static InviteEvent newParticipant(Invite invite, Long creatorId, Long newParticipantUserId) {
        return new InviteEvent(invite, InviteEventType.NEW_PARTICIPANT, invite, newParticipantUserId,
                Collections.singletonList(creatorId), null);
    }

    public static InviteEvent participantLeave(Invite invite, Long leaverUserId, Long creatorId) {
        return new InviteEvent(invite, InviteEventType.PARTICIPANT_LEAVE, invite, leaverUserId,
                Collections.singletonList(creatorId), null);
    }

    public static InviteEvent inviteCancelled(Invite invite, Long cancellerUserId, List<Long> participantIds, String reason) {
        return new InviteEvent(invite, InviteEventType.INVITE_CANCELLED, invite, cancellerUserId, participantIds, reason);
    }

    public static InviteEvent waitMatch(Invite invite, Long waitUserId) {
        return new InviteEvent(invite, InviteEventType.WAIT_MATCH, invite, waitUserId,
                Collections.singletonList(waitUserId), null);
    }

    /** 发起人踢出参与者（targetUserIds 为被踢用户 ID 列表，仅一个） */
    public static InviteEvent participantKicked(Invite invite, Long creatorUserId, Long kickedUserId, String reason) {
        return new InviteEvent(invite, InviteEventType.PARTICIPANT_KICKED, invite, creatorUserId,
                Collections.singletonList(kickedUserId), reason);
    }

    public enum InviteEventType {
        JOIN_SUCCESS,
        NEW_PARTICIPANT,
        PARTICIPANT_LEAVE,
        PARTICIPANT_KICKED,
        INVITE_CANCELLED,
        WAIT_MATCH
    }
}
