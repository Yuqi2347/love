package com.campus.love.invite.event;

import com.campus.love.invite.event.InviteEvent.InviteEventType;
import com.campus.love.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 邀约领域事件监听：根据事件类型发送站内通知，与 InviteService 解耦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationInviteEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onInviteEvent(InviteEvent event) {
        if (event.getInvite() == null) {
            return;
        }
        try {
            switch (event.getType()) {
                case JOIN_SUCCESS:
                    notificationService.notifyInviteJoinSuccess(
                            event.getActorUserId(), event.getActorUserId(), event.getInvite());
                    break;
                case NEW_PARTICIPANT:
                    if (!event.getTargetUserIds().isEmpty()) {
                        notificationService.notifyInviteNewParticipant(
                                event.getTargetUserIds().get(0), event.getActorUserId(), event.getInvite());
                    }
                    break;
                case PARTICIPANT_LEAVE:
                    if (!event.getTargetUserIds().isEmpty()) {
                        notificationService.notifyParticipantLeave(
                                event.getTargetUserIds().get(0), event.getActorUserId(), event.getInvite());
                    }
                    break;
                case INVITE_CANCELLED:
                    notificationService.notifyInviteCancelled(
                            event.getTargetUserIds(), event.getActorUserId(), event.getInvite(), event.getReason());
                    break;
                case WAIT_MATCH:
                    notificationService.notifyWaitMatch(event.getActorUserId(), event.getInvite());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("邀约事件通知发送失败, type={}, inviteId={}", event.getType(), event.getInvite().getId(), e);
        }
    }
}
