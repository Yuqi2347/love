package com.campus.love.moment.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 当前（最多一个）匹配任务进度，供管理端 /match/progress 读取；simulate 不使用此注册表。
 */
@Component
public class MomentMatchProgressRegistry {

    public record Snapshot(
            String weekTag,
            MomentMatcher.MatchProgressContext progressContext,
            long totalEstimatedPairs,
            String currentPool,
            int matchedPairsSoFar
    ) {
    }

    private final AtomicReference<Snapshot> active = new AtomicReference<>();

    public void start(String weekTag, MomentMatcher.MatchProgressContext ctx, long totalEstimatedPairs) {
        active.set(new Snapshot(weekTag, ctx, totalEstimatedPairs, "", 0));
    }

    public void addEstimatedPairs(long delta) {
        Snapshot s = active.get();
        if (s == null) {
            return;
        }
        active.set(new Snapshot(
                s.weekTag,
                s.progressContext,
                Math.max(0L, s.totalEstimatedPairs + Math.max(0L, delta)),
                s.currentPool,
                s.matchedPairsSoFar
        ));
    }

    public void setCurrentPool(String pool) {
        Snapshot s = active.get();
        if (s == null) {
            return;
        }
        active.set(new Snapshot(s.weekTag, s.progressContext, s.totalEstimatedPairs, pool, s.matchedPairsSoFar));
    }

    public void setMatchedPairsSoFar(int n) {
        Snapshot s = active.get();
        if (s == null) {
            return;
        }
        active.set(new Snapshot(s.weekTag, s.progressContext, s.totalEstimatedPairs, s.currentPool, n));
    }

    public void clear() {
        active.set(null);
    }

    public Snapshot getSnapshot() {
        return active.get();
    }
}
