package com.campus.love.match.service;

import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.OceanConfidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OceanMatcher {

    private final OceanConfidenceService oceanConfidenceService;

    public int calculateOceanScore(UserPortrait selfPortrait, UserPortrait targetPortrait) {
        if (selfPortrait == null || targetPortrait == null || !Boolean.TRUE.equals(selfPortrait.getHasRealOcean())) {
            return 50;
        }

        Map<String, BigDecimal> selfOcean = oceanConfidenceService.getEffectiveOcean(selfPortrait);
        Map<String, BigDecimal> targetOcean = oceanConfidenceService.getEffectiveOcean(targetPortrait);

        double totalDiff = 0.0;
        int count = 0;
        for (String dim : new String[]{"O", "C", "E", "A", "N"}) {
            BigDecimal selfValue = selfOcean.get(dim);
            BigDecimal targetValue = targetOcean.get(dim);
            if (selfValue == null || targetValue == null) {
                continue;
            }
            totalDiff += Math.abs(selfValue.doubleValue() - targetValue.doubleValue());
            count++;
        }

        if (count == 0) {
            return 50;
        }

        double similarity = 1.0 - (totalDiff / count) / 100.0;
        return (int) Math.round(Math.max(0.0, Math.min(1.0, similarity)) * 100.0);
    }
}
