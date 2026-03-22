package com.campus.love.pairdate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.ai.skill.DateOptionSkill;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.RedisKeyConstants;
import com.campus.love.common.enums.NotificationTypeEnum;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.follow.service.FollowService;
import com.campus.love.invite.service.MomentPairInviteService;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentMatchResultMapper;
import com.campus.love.moment.mapper.MomentProfileMapper;
import com.campus.love.moment.service.MomentService;
import com.campus.love.notification.service.NotificationService;
import com.campus.love.pairdate.dto.PairDateNegotiationVO;
import com.campus.love.pairdate.dto.PairDateSubmitRequest;
import com.campus.love.pairdate.dto.PairDateTimeVO;
import com.campus.love.pairdate.entity.MomentYueIntent;
import com.campus.love.pairdate.entity.PairDateNegotiation;
import com.campus.love.pairdate.enums.LocationChoice;
import com.campus.love.pairdate.enums.PairDateStatus;
import com.campus.love.pairdate.enums.TimeSlotCode;
import com.campus.love.pairdate.mapper.MomentYueIntentMapper;
import com.campus.love.pairdate.mapper.PairDateNegotiationMapper;
import com.campus.love.pairdate.util.PairDateTimeUtils;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairDateService {

    private final PairDateNegotiationMapper negotiationMapper;
    private final MomentYueIntentMapper yueIntentMapper;
    private final MomentMatchResultMapper matchResultMapper;
    private final FollowService followService;
    private final DateOptionSkill dateOptionSkill;
    private final UserMapper userMapper;
    private final MomentProfileMapper momentProfileMapper;
    private final UserPortraitService userPortraitService;
    private final PairDateResultService resultService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final MomentService momentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MomentPairInviteService momentPairInviteService;

    @Transactional
    public PairDateNegotiationVO yue(Long matchResultId) {
        Long me = CurrentUser.getId();
        MomentMatchResult match = matchResultMapper.selectById(matchResultId);
        if (match == null || (!Objects.equals(match.getUserIdA(), me) && !Objects.equals(match.getUserIdB(), me))) {
            throw new BusinessException(ResultCode.PAIR_DATE_MATCH_NOT_FOUND);
        }
        Long other = Objects.equals(match.getUserIdA(), me) ? match.getUserIdB() : match.getUserIdA();
        followService.mutualFollow(me, other);

        PairDateNegotiation existing = negotiationMapper.selectOne(
                new LambdaQueryWrapper<PairDateNegotiation>().eq(PairDateNegotiation::getMatchResultId, matchResultId));
        if (existing != null) {
            if (PairDateStatus.EXPIRED.name().equals(existing.getStatus())) {
                yueIntentMapper.delete(new LambdaQueryWrapper<MomentYueIntent>()
                        .eq(MomentYueIntent::getMatchResultId, matchResultId));
                negotiationMapper.deleteById(existing.getId());
                existing = null;
            } else if (PairDateStatus.TIME_MISMATCH.name().equals(existing.getStatus())) {
                throw new BusinessException(ResultCode.PAIR_DATE_CLOSED, "本周时间未匹配，暂不可再次发起协商");
            } else if (terminalBlocking(existing.getStatus())) {
                return buildVo(existing, me);
            }
        }

        MomentYueIntent intent = new MomentYueIntent();
        intent.setMatchResultId(matchResultId);
        intent.setUserId(me);
        try {
            yueIntentMapper.insert(intent);
        } catch (DuplicateKeyException ignored) {
            // 已记录意向
        }

        long cnt = yueIntentMapper.selectCount(new LambdaQueryWrapper<MomentYueIntent>()
                .eq(MomentYueIntent::getMatchResultId, matchResultId));
        if (cnt == 1) {
            User self = userMapper.selectById(me);
            String nick = self != null && self.getNickname() != null ? self.getNickname() : "Ta";
            notificationService.createNotification(other, me, null, null, null, null,
                    NotificationTypeEnum.PAIR_DATE_YUE_INTENT,
                    "有人想和你约一下",
                    nick + " 在心动匹配里点了「约一下」，打开心动结果页回应吧");
        }

        if (cnt >= 2) {
            PairDateNegotiation again = negotiationMapper.selectOne(
                    new LambdaQueryWrapper<PairDateNegotiation>().eq(PairDateNegotiation::getMatchResultId, matchResultId));
            if (again == null) {
                createNegotiationRow(match, me);
            }
        }

        PairDateNegotiation neg = negotiationMapper.selectOne(
                new LambdaQueryWrapper<PairDateNegotiation>().eq(PairDateNegotiation::getMatchResultId, matchResultId));
        if (neg == null) {
            return PairDateNegotiationVO.builder()
                    .matchResultId(matchResultId)
                    .status("WAITING_PARTNER_YUE")
                    .build();
        }
        return buildVo(neg, me);
    }

    private static boolean terminalBlocking(String status) {
        return PairDateStatus.COMPLETED.name().equals(status)
                || PairDateStatus.CALCULATING.name().equals(status);
    }

    private void createNegotiationRow(MomentMatchResult match, Long currentUserId) {
        MomentYueIntent first = yueIntentMapper.selectOne(
                new LambdaQueryWrapper<MomentYueIntent>()
                        .eq(MomentYueIntent::getMatchResultId, match.getId())
                        .orderByAsc(MomentYueIntent::getId)
                        .last("LIMIT 1"));
        Long firstYueUserId = first != null ? first.getUserId() : currentUserId;

        User ua = userMapper.selectById(match.getUserIdA());
        User ub = userMapper.selectById(match.getUserIdB());
        MomentProfile pa = momentProfileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, match.getUserIdA()));
        MomentProfile pb = momentProfileMapper.selectOne(
                new LambdaQueryWrapper<MomentProfile>().eq(MomentProfile::getUserId, match.getUserIdB()));
        UserPortrait pta = userPortraitService.getPortrait(match.getUserIdA());
        UserPortrait ptb = userPortraitService.getPortrait(match.getUserIdB());

        String dateJson = dateOptionSkill.generateDateOptionsJson(ua, ub, pa, pb, pta, ptb);

        PairDateNegotiation row = new PairDateNegotiation();
        row.setMatchResultId(match.getId());
        row.setUserIdA(match.getUserIdA());
        row.setUserIdB(match.getUserIdB());
        row.setFirstYueUserId(firstYueUserId);
        row.setWeekTag(match.getWeekTag());
        row.setDateOptions(dateJson);
        row.setStatus(PairDateStatus.PENDING.name());
        row.setVersion(0);
        row.setTimeMismatch(false);
        try {
            negotiationMapper.insert(row);
        } catch (DuplicateKeyException e) {
            log.debug("并发创建协商记录，已存在: matchResultId={}", match.getId());
        }
    }

    public PairDateNegotiationVO getById(Long negotiationId) {
        Long me = CurrentUser.getId();
        PairDateNegotiation n = negotiationMapper.selectById(negotiationId);
        if (n == null) {
            throw new BusinessException(ResultCode.PAIR_DATE_NEGOTIATION_NOT_FOUND);
        }
        assertParticipant(n, me);
        return buildVo(n, me);
    }

    public PairDateNegotiationVO getByPairTarget(Long targetUserId) {
        Long me = CurrentUser.getId();
        String week = momentService.getCurrentWeekTag();
        PairDateNegotiation n = negotiationMapper.selectOne(
                new LambdaQueryWrapper<PairDateNegotiation>()
                        .eq(PairDateNegotiation::getWeekTag, week)
                        .and(w -> w
                                .eq(PairDateNegotiation::getUserIdA, me)
                                .eq(PairDateNegotiation::getUserIdB, targetUserId)
                                .or()
                                .eq(PairDateNegotiation::getUserIdA, targetUserId)
                                .eq(PairDateNegotiation::getUserIdB, me))
        );
        if (n == null) {
            return null;
        }
        return buildVo(n, me);
    }

    public PairDateTimeVO getTime(Long negotiationId) {
        Long me = CurrentUser.getId();
        PairDateNegotiation n = negotiationMapper.selectById(negotiationId);
        if (n == null) {
            throw new BusinessException(ResultCode.PAIR_DATE_NEGOTIATION_NOT_FOUND);
        }
        assertParticipant(n, me);
        return PairDateTimeVO.builder()
                .meetingTimestamp(n.getMeetingTimestamp())
                .serverTime(System.currentTimeMillis())
                .build();
    }

    @Transactional
    public PairDateNegotiationVO submit(Long negotiationId, PairDateSubmitRequest req) {
        Long me = CurrentUser.getId();
        String lockKey = RedisKeyConstants.pairDateSubmit(me, negotiationId);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ResultCode.PAIR_DATE_SUBMIT_BUSY);
        }
        try {
            PairDateNegotiation row = negotiationMapper.selectById(negotiationId);
            if (row == null) {
                throw new BusinessException(ResultCode.PAIR_DATE_NEGOTIATION_NOT_FOUND);
            }
            assertParticipant(row, me);
            assertEditable(row);

            boolean isA = row.getUserIdA().equals(me);
            applyStep(row, isA, req);

            negotiationMapper.updateById(row);

            PairDateNegotiation fresh = negotiationMapper.selectById(negotiationId);
            boolean aDone = sideAComplete(fresh);
            boolean bDone = sideBComplete(fresh);
            String st = fresh.getStatus();

            if (aDone && bDone && (PairDateStatus.SIDE_A_DONE.name().equals(st) || PairDateStatus.SIDE_B_DONE.name().equals(st))) {
                tryFinalize(fresh);
            } else if (aDone && PairDateStatus.PENDING.name().equals(st)) {
                fresh.setStatus(PairDateStatus.SIDE_A_DONE.name());
                negotiationMapper.updateById(fresh);
            } else if (bDone && PairDateStatus.PENDING.name().equals(st)) {
                fresh.setStatus(PairDateStatus.SIDE_B_DONE.name());
                negotiationMapper.updateById(fresh);
            }

            PairDateNegotiation latest = negotiationMapper.selectById(negotiationId);
            return buildVo(latest, me);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void tryFinalize(PairDateNegotiation row) {
        String statusBeforeCalc = row.getStatus();
        int u = negotiationMapper.update(null, new LambdaUpdateWrapper<PairDateNegotiation>()
                .eq(PairDateNegotiation::getId, row.getId())
                .in(PairDateNegotiation::getStatus, PairDateStatus.SIDE_A_DONE.name(), PairDateStatus.SIDE_B_DONE.name())
                .eq(PairDateNegotiation::getVersion, row.getVersion())
                .set(PairDateNegotiation::getStatus, PairDateStatus.CALCULATING.name())
                .set(PairDateNegotiation::getVersion, row.getVersion() + 1));
        if (u == 0) {
            return;
        }
        PairDateNegotiation calc = negotiationMapper.selectById(row.getId());
        try {
            JsonNode finalOpt = resultService.pickFinalDateOption(
                    calc.getDateOptions(), calc.getAExcludedOption(), calc.getBExcludedOption());
            TimeSlotCode slot = resultService.minIntersectSlots(calc.getATimeSlots(), calc.getBTimeSlots());
            LocationChoice lcA = LocationChoice.valueOf(calc.getALocationChoice());
            LocationChoice lcB = LocationChoice.valueOf(calc.getBLocationChoice());
            long decider = resultService.calcLocationDeciderId(calc.getUserIdA(), calc.getUserIdB(), lcA, lcB);
            String reason = resultService.calcDeciderReasonKey(lcA, lcB, decider, calc.getUserIdA(), calc.getUserIdB());

            boolean mismatch = slot == null;
            String endStatus = mismatch ? PairDateStatus.TIME_MISMATCH.name() : PairDateStatus.COMPLETED.name();
            Long meetingTs = mismatch ? null : PairDateTimeUtils.slotStartEpochMillis(calc.getWeekTag(), slot);
            String slotName = mismatch ? null : slot.name();

            negotiationMapper.update(null, new LambdaUpdateWrapper<PairDateNegotiation>()
                    .eq(PairDateNegotiation::getId, calc.getId())
                    .eq(PairDateNegotiation::getStatus, PairDateStatus.CALCULATING.name())
                    .eq(PairDateNegotiation::getVersion, calc.getVersion())
                    .set(PairDateNegotiation::getFinalDateOption, objectMapper.writeValueAsString(finalOpt))
                    .set(PairDateNegotiation::getMeetingTimeSlot, slotName)
                    .set(PairDateNegotiation::getMeetingTimestamp, meetingTs)
                    .set(PairDateNegotiation::getLocationDeciderId, decider)
                    .set(PairDateNegotiation::getDeciderReasonKey, reason)
                    .set(PairDateNegotiation::getTimeMismatch, mismatch)
                    .set(PairDateNegotiation::getStatus, endStatus)
                    .set(PairDateNegotiation::getVersion, calc.getVersion() + 1));

            PairDateNegotiation done = negotiationMapper.selectById(calc.getId());
            try {
                Long invId = momentPairInviteService.createFromPairNegotiation(done);
                if (invId != null) {
                    negotiationMapper.update(null, new LambdaUpdateWrapper<PairDateNegotiation>()
                            .eq(PairDateNegotiation::getId, done.getId())
                            .set(PairDateNegotiation::getPairInviteId, invId));
                    done.setPairInviteId(invId);
                }
            } catch (Exception ex) {
                log.error("pair-date create 1v1 invite failed negId={}", done.getId(), ex);
            }
            notifyResultReady(done);
        } catch (Exception e) {
            log.error("pair-date finalize failed id={}", row.getId(), e);
            negotiationMapper.update(null, new LambdaUpdateWrapper<PairDateNegotiation>()
                    .eq(PairDateNegotiation::getId, row.getId())
                    .eq(PairDateNegotiation::getStatus, PairDateStatus.CALCULATING.name())
                    .eq(PairDateNegotiation::getVersion, row.getVersion() + 1)
                    .set(PairDateNegotiation::getStatus, statusBeforeCalc)
                    .set(PairDateNegotiation::getVersion, row.getVersion()));
        }
    }

    private void notifyResultReady(PairDateNegotiation n) {
        if (n == null) {
            return;
        }
        String title = "心动约会协商已有结果";
        String content = PairDateStatus.TIME_MISMATCH.name().equals(n.getStatus())
                ? "你们的时间没有完全对上，约会方式与地点决定者已生成，请查看"
                : "约会方式与时间已匹配好，快去看看专属邀约卡片吧";
        Long rid = n.getId();
        notificationService.createNotification(n.getUserIdA(), null, null, null, null, rid,
                NotificationTypeEnum.PAIR_DATE_RESULT_READY, title, content);
        notificationService.createNotification(n.getUserIdB(), null, null, null, null, rid,
                NotificationTypeEnum.PAIR_DATE_RESULT_READY, title, content);
    }

    private void applyStep(PairDateNegotiation row, boolean isA, PairDateSubmitRequest req) {
        int step = req.getStep();
        if (step == 1) {
            if (req.getExcludedRank() == null) {
                throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP, "请提交要排除的约会方式序号");
            }
            int r = req.getExcludedRank();
            if (r < 1 || r > 3) {
                throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP);
            }
            if (isA) {
                if (row.getAExcludedOption() != null && !row.getAExcludedOption().equals(r)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第一步已选择，暂不支持修改");
                }
                row.setAExcludedOption(r);
            } else {
                if (row.getBExcludedOption() != null && !row.getBExcludedOption().equals(r)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第一步已选择，暂不支持修改");
                }
                row.setBExcludedOption(r);
            }
            return;
        }
        if (step == 2) {
            if (req.getTimeSlots() == null || req.getTimeSlots().isEmpty()) {
                throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP, "请至少选择一个时段");
            }
            try {
                for (String c : req.getTimeSlots()) {
                    TimeSlotCode.fromString(c);
                }
            } catch (Exception e) {
                throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP, "时段编码无效");
            }
            String json;
            try {
                json = objectMapper.writeValueAsString(req.getTimeSlots());
            } catch (Exception e) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR);
            }
            if (isA) {
                if (row.getATimeSlots() != null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第二步已选择，暂不支持修改");
                }
                row.setATimeSlots(json);
            } else {
                if (row.getBTimeSlots() != null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第二步已选择，暂不支持修改");
                }
                row.setBTimeSlots(json);
            }
            return;
        }
        if (step == 3) {
            LocationChoice lc;
            try {
                lc = LocationChoice.valueOf(req.getLocationChoice());
            } catch (Exception e) {
                throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP, "地点选择无效");
            }
            if (isA) {
                if (row.getALocationChoice() != null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第三步已选择，暂不支持修改");
                }
                row.setALocationChoice(lc.name());
            } else {
                if (row.getBLocationChoice() != null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "第三步已选择，暂不支持修改");
                }
                row.setBLocationChoice(lc.name());
            }
            return;
        }
        throw new BusinessException(ResultCode.PAIR_DATE_BAD_STEP);
    }

    private static boolean sideAComplete(PairDateNegotiation n) {
        return n.getAExcludedOption() != null
                && n.getATimeSlots() != null && !n.getATimeSlots().isBlank()
                && n.getALocationChoice() != null;
    }

    private static boolean sideBComplete(PairDateNegotiation n) {
        return n.getBExcludedOption() != null
                && n.getBTimeSlots() != null && !n.getBTimeSlots().isBlank()
                && n.getBLocationChoice() != null;
    }

    private static void assertParticipant(PairDateNegotiation n, Long userId) {
        if (!n.getUserIdA().equals(userId) && !n.getUserIdB().equals(userId)) {
            throw new BusinessException(ResultCode.PAIR_DATE_FORBIDDEN);
        }
    }

    private static void assertEditable(PairDateNegotiation n) {
        String s = n.getStatus();
        if (PairDateStatus.COMPLETED.name().equals(s)
                || PairDateStatus.TIME_MISMATCH.name().equals(s)
                || PairDateStatus.EXPIRED.name().equals(s)
                || PairDateStatus.CALCULATING.name().equals(s)) {
            throw new BusinessException(ResultCode.PAIR_DATE_CLOSED);
        }
    }

    private PairDateNegotiationVO buildVo(PairDateNegotiation n, Long viewerId) {
        boolean isA = n.getUserIdA().equals(viewerId);
        boolean reveal = shouldRevealPartner(n.getStatus());
        boolean aDone = sideAComplete(n);
        boolean bDone = sideBComplete(n);
        boolean partnerDone = isA ? bDone : aDone;

        JsonNode dateOpts;
        try {
            dateOpts = objectMapper.readTree(n.getDateOptions());
        } catch (Exception e) {
            dateOpts = objectMapper.createObjectNode();
        }

        PairDateNegotiationVO.PairDateNegotiationVOBuilder b = PairDateNegotiationVO.builder()
                .id(n.getId())
                .matchResultId(n.getMatchResultId())
                .status(n.getStatus())
                .weekTag(n.getWeekTag())
                .dateOptions(dateOpts)
                .iAmUserA(isA)
                .myExcludedRank(isA ? n.getAExcludedOption() : n.getBExcludedOption())
                .myTimeSlots(readSlots(isA ? n.getATimeSlots() : n.getBTimeSlots()))
                .myLocationChoice(isA ? n.getALocationChoice() : n.getBLocationChoice())
                .partnerFinishedAll(partnerDone)
                .revealPartnerChoices(reveal)
                .meetingTimeSlot(n.getMeetingTimeSlot())
                .meetingTimestamp(n.getMeetingTimestamp())
                .locationDeciderId(n.getLocationDeciderId())
                .deciderReasonKey(n.getDeciderReasonKey())
                .timeMismatch(n.getTimeMismatch());

        if (reveal) {
            b.partnerExcludedRank(isA ? n.getBExcludedOption() : n.getAExcludedOption())
                    .partnerTimeSlots(readSlots(isA ? n.getBTimeSlots() : n.getATimeSlots()))
                    .partnerLocationChoice(isA ? n.getBLocationChoice() : n.getALocationChoice());
        }
        if (n.getFinalDateOption() != null && !n.getFinalDateOption().isBlank()) {
            try {
                b.finalDateOption(objectMapper.readTree(n.getFinalDateOption()));
            } catch (Exception ignored) {
            }
        }

        Long deciderId = n.getLocationDeciderId();
        Long guestUid = null;
        String initiatorNick = null;
        String guestNick = null;
        if (deciderId != null) {
            guestUid = n.getUserIdA().equals(deciderId) ? n.getUserIdB() : n.getUserIdA();
            User uDec = userMapper.selectById(deciderId);
            User uGuest = guestUid != null ? userMapper.selectById(guestUid) : null;
            String initAv = null;
            String guestAv = null;
            if (uDec != null) {
                initiatorNick = uDec.getNickname();
                initAv = uDec.getAvatarUrl();
            }
            if (uGuest != null) {
                guestNick = uGuest.getNickname();
                guestAv = uGuest.getAvatarUrl();
            }
            b.initiatorAvatarUrl(initAv).guestAvatarUrl(guestAv);
        }
        b.pairInviteId(n.getPairInviteId())
                .initiatorNickname(initiatorNick)
                .guestNickname(guestNick)
                .guestUserId(guestUid);

        return b.build();
    }

    private static boolean shouldRevealPartner(String status) {
        return PairDateStatus.COMPLETED.name().equals(status)
                || PairDateStatus.TIME_MISMATCH.name().equals(status)
                || PairDateStatus.EXPIRED.name().equals(status);
    }

    private List<String> readSlots(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
