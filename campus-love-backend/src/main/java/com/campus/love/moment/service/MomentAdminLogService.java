package com.campus.love.moment.service;

import com.campus.love.moment.entity.MomentAdminLog;
import com.campus.love.moment.mapper.MomentAdminLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MomentAdminLogService {

    private final MomentAdminLogMapper adminLogMapper;

    public void log(String weekTag,
                    Long operatorId,
                    String actionType,
                    String targetType,
                    Long targetId,
                    String summary,
                    String detailJson) {
        MomentAdminLog log = new MomentAdminLog();
        log.setWeekTag(weekTag);
        log.setOperatorId(operatorId);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setSummary(summary);
        log.setDetailJson(detailJson);
        adminLogMapper.insert(log);
    }
}
