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
        return config;
    }

    private MomentMatchConfig defaultConfig() {
        MomentMatchConfig config = new MomentMatchConfig();
        config.setId(MomentMatchConfig.DEFAULT_ID);
        config.setBaseThreshold(MomentMatchConfig.DEFAULT_BASE_THRESHOLD);
        config.setPrioritizeOffset(MomentMatchConfig.DEFAULT_PRIORITIZE_OFFSET);
        config.setPriorityOffset(MomentMatchConfig.DEFAULT_PRIORITY_OFFSET);
        config.setPriorityMaxStack(MomentMatchConfig.DEFAULT_PRIORITY_MAX_STACK);
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
}
