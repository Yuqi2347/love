package com.campus.love.moment.service;

import com.campus.love.ai.agent.MomentAboutAgent;
import com.campus.love.ai.agent.MomentDatePrepAgent;
import com.campus.love.ai.agent.MomentInsightAgent;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentResultContentService {

    private final ComplementaryModeService complementaryModeService;
    private final DateSceneTypeResolver dateSceneTypeResolver;
    private final MomentDimensionLabelResolver dimensionLabelResolver;
    private final MomentInsightAgent momentInsightAgent;
    private final MomentAboutAgent momentAboutAgent;
    private final MomentDatePrepAgent momentDatePrepAgent;
    private final ObjectMapper objectMapper;

    public void fillPrecomputedContent(
            MomentMatchResult result,
            User userA,
            User userB,
            MomentProfile profileA,
            MomentProfile profileB,
            UserPortrait portraitA,
            UserPortrait portraitB,
            Map<String, Object> scoreDetail
    ) {
        List<String> modes = complementaryModeService.resolveModes(profileA, profileB);
        List<String> softPenaltyReasons = parseStringList(scoreDetail != null ? scoreDetail.get("softPenaltyReasons") : null);
        String dateSceneType = dateSceneTypeResolver.resolve(
                profileA != null ? profileA.getDateStyle() : null,
                profileB != null ? profileB.getDateStyle() : null
        );
        List<String> dimensionLabels = dimensionLabelResolver.resolveLabels(scoreDetail);
        MomentInsightAgent.InsightResult insight = momentInsightAgent.generate(
                userA, userB, profileA, profileB, portraitA, portraitB, modes, softPenaltyReasons
        );
        String aboutA = momentAboutAgent.generate(userA, profileA, portraitA);
        String aboutB = momentAboutAgent.generate(userB, profileB, portraitB);

        result.setYuanfenTitle(complementaryModeService.resolvePrimaryTitle(profileA, profileB));
        result.setComplementaryModes(writeJson(modes));
        result.setSoftPenaltyReasons(writeJson(softPenaltyReasons));
        result.setDateSceneType(dateSceneType);
        result.setInsightCard1(insight.card1);
        result.setInsightCard2(insight.card2);
        result.setInsightCard3(insight.card3);
        result.setGoldenSentence(insight.goldenSentence);
        result.setDimensionLabels(writeJson(dimensionLabels));
        result.setAboutUserA(aboutA);
        result.setAboutUserB(aboutB);
    }

    public MomentDatePrepResponse getOrGenerateDatePrep(
            MomentMatchResult result,
            User requester,
            User target,
            MomentProfile requesterProfile,
            MomentProfile targetProfile,
            UserPortrait requesterPortrait,
            UserPortrait targetPortrait
    ) {
        MomentDatePrepResponse cached = parseDatePrep(result.getDatePrepJson());
        if (cached != null) {
            return cached;
        }
        MomentDatePrepResponse generated = momentDatePrepAgent.generate(
                requester,
                target,
                requesterProfile,
                targetProfile,
                requesterPortrait,
                targetPortrait,
                result.getDateSceneType()
        );
        result.setDatePrepJson(writeJson(generated));
        return generated;
    }

    public List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("parseJsonList failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> parseStringList(Object source) {
        if (source == null) {
            return List.of();
        }
        if (source instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return parseJsonList(String.valueOf(source));
    }

    public List<String> buildInsightCards(MomentMatchResult result) {
        List<String> cards = new ArrayList<>();
        if (result.getInsightCard1() != null && !result.getInsightCard1().isBlank()) {
            cards.add(result.getInsightCard1());
        }
        if (result.getInsightCard2() != null && !result.getInsightCard2().isBlank()) {
            cards.add(result.getInsightCard2());
        }
        if (result.getInsightCard3() != null && !result.getInsightCard3().isBlank()) {
            cards.add(result.getInsightCard3());
        }
        return cards;
    }

    public MomentDatePrepResponse parseDatePrep(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MomentDatePrepResponse.class);
        } catch (Exception e) {
            log.warn("parseDatePrep failed: {}", e.getMessage());
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("writeJson failed: {}", e.getMessage());
            return null;
        }
    }
}
