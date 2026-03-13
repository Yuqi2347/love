package com.campus.love.relation.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.follow.entity.Follow;
import com.campus.love.follow.mapper.FollowMapper;
import com.campus.love.relation.entity.RelationMilestone;
import com.campus.love.relation.mapper.RelationMilestoneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 关系节点规则引擎（技术文档 V1.1.0 第 12 节）
 * 每日早9点检测互关天数（1/3/7/30天），写入 t_relation_milestone
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationMilestoneScheduler {

    private final FollowMapper followMapper;
    private final RelationMilestoneMapper relationMilestoneMapper;

    private static final int[] MILESTONE_DAYS = {1, 3, 7, 30};

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkMilestones() {
        log.info("Relation milestone check starting");
        try {
            List<Follow> mutualFollows = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getIsMutual, true));
            if (mutualFollows == null) return;

            for (Follow f : mutualFollows) {
                Long a = Math.min(f.getFollowerId(), f.getFollowingId());
                Long b = Math.max(f.getFollowerId(), f.getFollowingId());
                long days = ChronoUnit.DAYS.between(f.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
                for (int d : MILESTONE_DAYS) {
                    if (days >= d) {
                        String type = "MUTUAL_" + d + "_DAY";
                        Long exists = relationMilestoneMapper.selectCount(
                                new LambdaQueryWrapper<RelationMilestone>()
                                        .eq(RelationMilestone::getUserIdA, a)
                                        .eq(RelationMilestone::getUserIdB, b)
                                        .eq(RelationMilestone::getMilestoneType, type));
                        if (exists == null || exists == 0) {
                            RelationMilestone m = new RelationMilestone();
                            m.setUserIdA(a);
                            m.setUserIdB(b);
                            m.setMilestoneType(type);
                            m.setNotifiedAt(LocalDateTime.now());
                            relationMilestoneMapper.insert(m);
                            log.info("Milestone {} for pair {}<->{}", type, a, b);
                        }
                    }
                }
            }
            log.info("Relation milestone check completed");
        } catch (Exception e) {
            log.error("Relation milestone check failed", e);
        }
    }
}
