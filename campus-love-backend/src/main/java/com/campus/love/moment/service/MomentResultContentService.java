package com.campus.love.moment.service;

import com.campus.love.ai.agent.MomentDatePrepAgent;
import com.campus.love.ai.agent.MomentResultPackAgent;
import com.campus.love.moment.dto.MomentDatePrepResponse;
import com.campus.love.moment.entity.MomentMatchResult;
import com.campus.love.moment.entity.MomentProfile;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final MomentResultPackAgent momentResultPackAgent;
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
        MomentResultPackAgent.ResultPack resultPack = momentResultPackAgent.generate(
                userA, userB, profileA, profileB, portraitA, portraitB, modes, softPenaltyReasons
        );

        result.setYuanfenTitle(complementaryModeService.resolvePrimaryTitle(profileA, profileB));
        result.setComplementaryModes(writeJson(modes));
        result.setSoftPenaltyReasons(writeJson(softPenaltyReasons));
        result.setDateSceneType(dateSceneType);
        result.setInsightCard1(resultPack.insight.card1);
        result.setInsightCard2(resultPack.insight.card2);
        result.setInsightCard3(resultPack.insight.card3);
        result.setGoldenSentence(resultPack.insight.goldenSentence);
        result.setDimensionLabels(writeJson(dimensionLabels));
        result.setAboutUserA(resultPack.aboutUserA);
        result.setAboutUserB(resultPack.aboutUserB);
    }

    public MomentDatePrepResponse getOrGenerateDatePrep(
            MomentMatchResult result,
            Long currentUserId,
            User requester,
            User target,
            MomentProfile requesterProfile,
            MomentProfile targetProfile,
            UserPortrait requesterPortrait,
            UserPortrait targetPortrait
    ) {
        String cacheKey = resolveDatePrepCacheKey(result, currentUserId);
        MomentDatePrepResponse cached = parseDatePrep(result.getDatePrepJson(), cacheKey);
        if (cached != null && !momentDatePrepAgent.shouldRefresh(cached)) {
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
        result.setDatePrepJson(mergeDatePrep(result.getDatePrepJson(), cacheKey, generated));
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

    public MomentDatePrepResponse parseDatePrep(String json, String cacheKey) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root != null && root.isObject() && cacheKey != null && root.has(cacheKey)) {
                return objectMapper.treeToValue(root.get(cacheKey), MomentDatePrepResponse.class);
            }
            if (root != null && root.isObject() && root.has("dateSuggestion")) {
                return null;
            }
        } catch (Exception e) {
            log.warn("parseDatePrepByKey failed: {}", e.getMessage());
        }
        return null;
    }

    private String mergeDatePrep(String rawJson, String cacheKey, MomentDatePrepResponse value) {
        try {
            JsonNode root = (rawJson == null || rawJson.isBlank()) ? objectMapper.createObjectNode() : objectMapper.readTree(rawJson);
            com.fasterxml.jackson.databind.node.ObjectNode objectNode;
            if (root != null && root.isObject() && !root.has("dateSuggestion")) {
                objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) root;
            } else {
                objectNode = objectMapper.createObjectNode();
            }
            objectNode.set(cacheKey, objectMapper.valueToTree(value));
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            log.warn("mergeDatePrep failed: {}", e.getMessage());
            return writeJson(value);
        }
    }

    private String resolveDatePrepCacheKey(MomentMatchResult result, Long currentUserId) {
        if (result == null || currentUserId == null) {
            return "userA";
        }
        return currentUserId.equals(result.getUserIdA()) ? "userA" : "userB";
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
