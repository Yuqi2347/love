package com.campus.love.moment.service;

import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.moment.entity.MomentMatchConfig;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.moment.mapper.MomentMatchConfigMapper;
import com.campus.love.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MomentMatchConfigService {

    private final MomentMatchConfigMapper matchConfigMapper;

    public MomentMatchConfig getConfig() {
        MomentMatchConfig config = matchConfigMapper.selectById(MomentMatchConfig.DEFAULT_ID);
        if (config == null) {
            config = defaultConfig();
            matchConfigMapper.insert(config);
            return config;
        }
        return normalize(config);
    }

    @Transactional
    public MomentMatchConfig saveConfig(MomentMatchConfig incoming) {
        if (incoming == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "匹配配置不能为空");
        }
        MomentMatchConfig current = getConfig();
        current.setBaseThreshold(requireNonNegative(incoming.getBaseThreshold(), "基础阈值"));
        current.setPrioritizeOffset(requireNonNegative(incoming.getPrioritizeOffset(), "优先匹配偏移"));
        current.setPriorityOffset(requireNonNegative(incoming.getPriorityOffset(), "优先权单次偏移"));
        current.setPriorityMaxStack(requireNonNegative(incoming.getPriorityMaxStack(), "优先权最大叠加次数"));
        current.setAutoMatchEnabled(incoming.getAutoMatchEnabled() != null ? incoming.getAutoMatchEnabled() : MomentMatchConfig.DEFAULT_AUTO_MATCH_ENABLED);
        current.setAutoMatchDayOfWeek(requireBetween(incoming.getAutoMatchDayOfWeek(), 1, 7, "自动匹配周几"));
        current.setAutoMatchTime(requireTimeHHmm(incoming.getAutoMatchTime(), "自动匹配时间"));
        matchConfigMapper.updateById(current);
        return current;
    }

    public int calculateEffectiveThreshold(User user, MomentProfile profile, MomentMatchConfig config) {
        int base = normalize(config).getBaseThreshold();
        int offset = calculateThresholdOffset(user, profile, config);
        return Math.max(0, base - offset);
    }

    public int calculateThresholdOffset(User user, MomentProfile profile, MomentMatchConfig config) {
        MomentMatchConfig normalized = normalize(config);
        int offset = Boolean.TRUE.equals(profile != null ? profile.getPrioritizeMatching() : null)
                ? normalized.getPrioritizeOffset()
                : 0;
        int priorityCount = user != null ? user.getMomentPriorityCountOrDefault() : 0;
        int stack = Math.min(priorityCount, normalized.getPriorityMaxStack());
        offset += normalized.getPriorityOffset() * Math.max(stack, 0);
        return offset;
    }

    private MomentMatchConfig normalize(MomentMatchConfig config) {
        if (config == null) {
            return defaultConfig();
        }
        if (config.getId() == null) {
            config.setId(MomentMatchConfig.DEFAULT_ID);
        }
        if (config.getBaseThreshold() == null) {
            config.setBaseThreshold(MomentMatchConfig.DEFAULT_BASE_THRESHOLD);
        }
        if (config.getPrioritizeOffset() == null) {
            config.setPrioritizeOffset(MomentMatchConfig.DEFAULT_PRIORITIZE_OFFSET);
        }
        if (config.getPriorityOffset() == null) {
            config.setPriorityOffset(MomentMatchConfig.DEFAULT_PRIORITY_OFFSET);
        }
        if (config.getPriorityMaxStack() == null) {
            config.setPriorityMaxStack(MomentMatchConfig.DEFAULT_PRIORITY_MAX_STACK);
        }
        if (config.getAutoMatchEnabled() == null) {
            config.setAutoMatchEnabled(MomentMatchConfig.DEFAULT_AUTO_MATCH_ENABLED);
        }
        if (config.getAutoMatchDayOfWeek() == null) {
            config.setAutoMatchDayOfWeek(MomentMatchConfig.DEFAULT_AUTO_MATCH_DAY_OF_WEEK);
        }
        if (config.getAutoMatchTime() == null || config.getAutoMatchTime().isBlank()) {
            config.setAutoMatchTime(MomentMatchConfig.DEFAULT_AUTO_MATCH_TIME);
        }
        return config;
    }

    private MomentMatchConfig defaultConfig() {
        MomentMatchConfig config = new MomentMatchConfig();
        config.setId(MomentMatchConfig.DEFAULT_ID);
        config.setBaseThreshold(MomentMatchConfig.DEFAULT_BASE_THRESHOLD);
        config.setPrioritizeOffset(MomentMatchConfig.DEFAULT_PRIORITIZE_OFFSET);
        config.setPriorityOffset(MomentMatchConfig.DEFAULT_PRIORITY_OFFSET);
        config.setPriorityMaxStack(MomentMatchConfig.DEFAULT_PRIORITY_MAX_STACK);
        config.setAutoMatchEnabled(MomentMatchConfig.DEFAULT_AUTO_MATCH_ENABLED);
        config.setAutoMatchDayOfWeek(MomentMatchConfig.DEFAULT_AUTO_MATCH_DAY_OF_WEEK);
        config.setAutoMatchTime(MomentMatchConfig.DEFAULT_AUTO_MATCH_TIME);
        return config;
    }

    private int requireNonNegative(Integer value, String label) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "不能为空");
        }
        if (value < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "不能小于 0");
        }
        return value;
    }

    private int requireBetween(Integer value, int min, int max, String label) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "不能为空");
        }
        if (value < min || value > max) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "必须在 " + min + "~" + max + " 之间");
        }
        return value;
    }

    private String requireTimeHHmm(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "不能为空");
        }
        if (!value.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "必须为 HH:mm");
        }
        return value;
    }
}
