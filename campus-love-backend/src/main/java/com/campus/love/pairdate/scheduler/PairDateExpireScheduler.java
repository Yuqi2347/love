package com.campus.love.pairdate.scheduler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.pairdate.entity.PairDateNegotiation;
import com.campus.love.pairdate.enums.PairDateStatus;
import com.campus.love.pairdate.mapper.PairDateNegotiationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 约会时段已过：COMPLETED → EXPIRED（对齐 V1.2.0_INVITATION_MODULE §7）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PairDateExpireScheduler {

    private static final long FIXED_DELAY_MS = 60_000L;

    private final PairDateNegotiationMapper negotiationMapper;

    @Scheduled(fixedDelay = FIXED_DELAY_MS)
    public void expirePastMeetings() {
        long now = System.currentTimeMillis();
        try {
            int n = negotiationMapper.update(null, new LambdaUpdateWrapper<PairDateNegotiation>()
                    .eq(PairDateNegotiation::getStatus, PairDateStatus.COMPLETED.name())
                    .isNotNull(PairDateNegotiation::getMeetingTimestamp)
                    .lt(PairDateNegotiation::getMeetingTimestamp, now)
                    .set(PairDateNegotiation::getStatus, PairDateStatus.EXPIRED.name()));
            if (n > 0) {
                log.info("pair-date 过期扫描：更新 {} 条为 EXPIRED", n);
            }
        } catch (Exception e) {
            log.warn("pair-date 过期扫描失败: {}", e.getMessage());
        }
    }
}
