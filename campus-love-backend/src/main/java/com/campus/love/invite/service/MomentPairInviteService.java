package com.campus.love.invite.service;

import com.campus.love.chat.service.ChatGroupService;
import com.campus.love.common.constants.DateTimeConstants;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.entity.InviteParticipant;
import com.campus.love.invite.enums.InviteModeEnum;
import com.campus.love.invite.enums.InvitePeriodEnum;
import com.campus.love.invite.enums.InviteStatusEnum;
import com.campus.love.invite.enums.InviteTypeEnum;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.mapper.InviteParticipantMapper;
import com.campus.love.pairdate.entity.PairDateNegotiation;
import com.campus.love.pairdate.enums.PairDateStatus;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 心动「约一下」协商结束后，生成一对一 {@link Invite}（发起人=地点决定者，受邀人=另一方）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentPairInviteService {

    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    private final InviteMapper inviteMapper;
    private final InviteParticipantMapper participantMapper;
    private final ChatGroupService chatGroupService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * @return 新建邀约 id；无需创建时返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFromPairNegotiation(PairDateNegotiation neg) {
        if (neg == null || neg.getPairInviteId() != null) {
            return null;
        }
        if (!PairDateStatus.COMPLETED.name().equals(neg.getStatus())
                && !PairDateStatus.TIME_MISMATCH.name().equals(neg.getStatus())) {
            return null;
        }
        Long decider = neg.getLocationDeciderId();
        if (decider == null) {
            return null;
        }
        Long other = Objects.equals(neg.getUserIdA(), decider) ? neg.getUserIdB() : neg.getUserIdA();

        String optionTitle = "心动专属约会";
        String optionDesc = "";
        try {
            if (neg.getFinalDateOption() != null && !neg.getFinalDateOption().isBlank()) {
                JsonNode opt = objectMapper.readTree(neg.getFinalDateOption());
                optionTitle = opt.path("title").asText(optionTitle);
                optionDesc = opt.path("description").asText("");
            }
        } catch (Exception e) {
            log.debug("parse finalDateOption: {}", e.getMessage());
        }

        User deciderUser = userMapper.selectById(decider);
        User otherUser = userMapper.selectById(other);
        String deciderNick = deciderUser != null && deciderUser.getNickname() != null ? deciderUser.getNickname() : "TA";
        String otherNick = otherUser != null && otherUser.getNickname() != null ? otherUser.getNickname() : "TA";

        LocalDateTime inviteTime;
        LocalDateTime inviteEndTime;
        if (neg.getMeetingTimestamp() != null && neg.getMeetingTimestamp() > 0) {
            inviteTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(neg.getMeetingTimestamp()), SH);
            inviteEndTime = inviteTime.plusHours(3);
        } else {
            inviteTime = LocalDateTime.now(SH).plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
            inviteEndTime = inviteTime.plusHours(3);
        }

        String title = "心动约会 · " + optionTitle;
        StringBuilder content = new StringBuilder();
        content.append("【Campus Love 心动时刻 · 一对一邀约】\n\n");
        content.append("· 发起人（由 Ta 定约会地点）：").append(deciderNick).append("。\n");
        content.append("· 受邀人：").append(otherNick).append("。\n\n");
        content.append("· 见面方式：").append(optionTitle);
        if (!optionDesc.isBlank()) {
            content.append("（").append(optionDesc).append("）");
        }
        content.append("。\n");
        if (Boolean.TRUE.equals(neg.getTimeMismatch())) {
            content.append("· 时间：双方空闲未完全重合，请私下约定具体时间。\n");
        } else if (neg.getMeetingTimeSlot() != null) {
            content.append("· 约定时段：").append(neg.getMeetingTimeSlot()).append("。\n");
        }
        content.append("· 地点：由 ").append(deciderNick).append(" 决定具体见面位置。\n\n");
        content.append("（协商记录编号 #").append(neg.getId()).append("）");

        Invite invite = new Invite();
        invite.setCreatorId(decider);
        invite.setTargetUserId(other);
        invite.setInviteMode(InviteModeEnum.PRIVATE.name());
        invite.setInviteType(InviteTypeEnum.OTHER.name());
        invite.setTitle(title);
        invite.setContent(content.toString());
        invite.setInvitePeriod(InvitePeriodEnum.ONCE.name());
        invite.setInviteTime(inviteTime);
        invite.setInviteEndTime(inviteEndTime);
        invite.setLocation("由 " + deciderNick + " 决定约会地点");
        invite.setCampus("ALL");
        invite.setMaxParticipants(1);
        invite.setParticipantCount(1);
        invite.setStatus(InviteStatusEnum.CONFIRMED.name());
        invite.setDeadlineHours(24);
        invite.setDeleted(false);
        inviteMapper.insert(invite);

        InviteParticipant p = new InviteParticipant();
        p.setInviteId(invite.getId());
        p.setUserId(other);
        p.setJoinAt(LocalDateTime.now());
        participantMapper.insert(p);

        var group = chatGroupService.createGroupIfAbsent(invite);
        invite.setChatGroupId(group.getId());
        inviteMapper.updateById(invite);
        chatGroupService.addMemberIfAbsent(group.getId(), decider);
        chatGroupService.addMemberIfAbsent(group.getId(), other);

        try {
            String timeStr = invite.getInviteTime() != null
                    ? invite.getInviteTime().format(DateTimeConstants.TIME_FMT)
                    : "";
            log.info("moment pair invite created id={} creator={} target={} time={}", invite.getId(), decider, other, timeStr);
        } catch (Exception ignored) {
        }

        return invite.getId();
    }
}
