package com.campus.love.ai.agent;

import com.campus.love.ai.service.AiService;
import com.campus.love.ai.service.YuanFenService;
import com.campus.love.common.utils.InterestTagConverter;
import com.campus.love.profile.entity.UserPortrait;
import com.campus.love.profile.service.OceanConfidenceService;
import com.campus.love.profile.service.UserPortraitService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 心动一刻总结报告 Agent（技术文档 V1.1.0 第 3.3 节）
 * 工具：get_questionnaire_highlight、get_ocean_compatibility、get_value_alignment
 * 支持缘分分析缓存注入。
 *
 * 架构说明：非严格 ReAct 循环。一次性调用工具构建上下文，再单次 AI 调用输出。
 * 同性配对：与破冰/缘分分析不同，心动一刻同性也以恋爱/心动方向描述。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentSummaryReActAgent {

    private final AiService aiService;
    private final YuanFenService yuanFenService;
    private final UserPortraitService userPortraitService;
    private final OceanConfidenceService oceanConfidenceService;
    private final UserMapper userMapper;

    private static final String SYSTEM_PROMPT = """
            你是 Campus Love 的「心动一刻配对分析师」。借鉴缘分分析的深度与格式，输出多段落、有温度的配对报告。

            ⚠️ 同性配对特殊说明：心动一刻与破冰、缘分分析不同。无论异性还是同性配对，均以恋爱/心动方向描述，温暖浪漫。同性配对不是友谊向，而是恋爱/心动向。

            你的准则：
            1. 有理有据：基于问卷亮点、OCEAN契合度、价值观、缘分分析（若有）进行数据支撑的推演。
            2. 深度剖析：不要泛泛而谈，要结合双方性格、兴趣、生活方式做具体分析。
            3. 电影级场景：预演一个校园/生活中的心动瞬间，注重细节与氛围。
            4. 语言质感：兼具学术的严谨与文学的浪漫，温暖而克制。

            输出结构（每段换行，共约400字）：
            ① 综合判词（约80字）：将匹配分数翻译成兼具理性和宿命感的综合评价。
            ② 性格与默契（约100字）：基于问卷与OCEAN，分析两人相处时的互补与共鸣。
            ③ 兴趣与生活方式（约80字）：共同爱好如何形成生活闭环，或不同爱好如何相互渗透。
            ④ 心动场景预演（约120字）：高颗粒度的校园/生活微电影片段，含具体地点、微动作、一句高智感对白。
            ⑤ 专属金句（约20字）：极具诗意和宿命感的收尾。

            直接输出上述5段文本，不要输出JSON或Markdown，不要输出任何解释性前言。
            """;

    /**
     * 生成心动一刻配对总结报告
     * @param totalScore 匹配综合分（可为null）
     */
    public String generateSummary(Long userAId, Long userBId, Double totalScore) {
        try {
            User uA = userMapper.selectById(userAId);
            User uB = userMapper.selectById(userBId);
            QuestionnaireHighlight qh = getQuestionnaireHighlight(userAId, userBId);
            OceanCompatibility oc = getOceanCompatibility(userAId, userBId);
            ValueAlignment va = getValueAlignment(userAId, userBId);

            StringBuilder context = new StringBuilder();
            if (totalScore != null) context.append("【匹配综合分】").append(String.format("%.0f", totalScore)).append("/100\n");
            context.append("【用户A】").append(formatUser(uA)).append("\n");
            context.append("【用户B】").append(formatUser(uB)).append("\n");
            context.append(String.format("问卷亮点：%s\nOCEAN契合：%s\n价值观：%s",
                    qh != null ? qh.summary() : "暂无",
                    oc != null ? oc.summary() : "暂无",
                    va != null ? va.summary() : "暂无"));

            yuanFenService.getCachedAnalysis(userAId, userBId).ifPresent(yf -> {
                context.append("\n【缘分分析（若有）】");
                if (yf.getYuanFenIndex() != null) context.append("\n缘分指数：").append(yf.getYuanFenIndex());
                if (yf.getOverallInterpretation() != null) context.append("\n总体评价：").append(yf.getOverallInterpretation());
                if (yf.getDevelopmentPotential() != null) context.append("\n发展可能：").append(yf.getDevelopmentPotential());
                else if (yf.getRelationshipPotential() != null) context.append("\n关系潜力：").append(yf.getRelationshipPotential());
            });

            var result = aiService.chatCompletion(SYSTEM_PROMPT, "【上下文】\n" + context + "\n\n请按5段结构输出配对报告。");
            if (result != null && result.getContent() != null) {
                return result.getContent().trim();
            }
        } catch (Exception e) {
            log.warn("MomentSummaryReActAgent failed: {}", e.getMessage());
        }
        return "你们在性格和兴趣上有很多共鸣，不妨从共同爱好聊起，慢慢了解彼此～";
    }

    private String formatUser(User u) {
        if (u == null) return "未知";
        var p = userPortraitService.getPortrait(u.getId());
        String interests = InterestTagConverter.getInterestsForDisplay(p != null ? p.getInterestTags() : null, u.getInterests());
        return String.format("%s | %s%s | %s | 兴趣:%s",
                u.getNickname() != null ? u.getNickname() : "匿名",
                u.getSchool() != null ? u.getSchool() : "",
                u.getGrade() != null ? u.getGrade() : "",
                u.getMajor() != null ? u.getMajor() : "未填",
                interests != null ? interests : "未填");
    }

    /** 工具：获取问卷最显著差异和契合点 */
    public QuestionnaireHighlight getQuestionnaireHighlight(Long userAId, Long userBId) {
        UserPortrait a = userPortraitService.getPortrait(userAId);
        UserPortrait b = userPortraitService.getPortrait(userBId);
        if (a == null || b == null) {
            return new QuestionnaireHighlight("问卷信息不足", List.of());
        }
        List<String> highlights = new ArrayList<>();
        if (same(a.getSocialStyle(), b.getSocialStyle())) highlights.add("社交风格相近");
        if (same(a.getLifeRhythm(), b.getLifeRhythm())) highlights.add("生活节奏相似");
        if (same(a.getPersonalityBase(), b.getPersonalityBase())) highlights.add("性格底色接近");
        if (same(a.getFutureLifestyle(), b.getFutureLifestyle())) highlights.add("未来生活方式同频");
        if (same(a.getConflictStyle(), b.getConflictStyle())) highlights.add("矛盾处理方式一致");
        if (same(a.getRelationshipCoreValue(), b.getRelationshipCoreValue())) highlights.add("核心价值观较一致");
        if (same(a.getDateStyle(), b.getDateStyle())) highlights.add("约会方式容易同拍");
        if (!same(a.getIntimacyPace(), b.getIntimacyPace()) && a.getIntimacyPace() != null && b.getIntimacyPace() != null) {
            highlights.add("亲密节奏存在互补空间");
        }
        if (highlights.isEmpty()) {
            highlights.add("问卷显示双方属于可磨合型组合");
        }
        String summary = highlights.size() >= 3 ? "问卷显示双方在相处节奏上较容易形成默契" : "问卷呈现出一定互补性，适合慢慢磨合";
        return new QuestionnaireHighlight(summary, highlights.stream().limit(4).toList());
    }

    /** 工具：OCEAN契合分析（使用有效 OCEAN） */
    public OceanCompatibility getOceanCompatibility(Long userAId, Long userBId) {
        UserPortrait pa = userPortraitService.getPortrait(userAId);
        UserPortrait pb = userPortraitService.getPortrait(userBId);
        if (pa == null || pb == null) {
            return null;
        }
        Map<String, BigDecimal> aOcean = oceanConfidenceService.getEffectiveOcean(pa);
        Map<String, BigDecimal> bOcean = oceanConfidenceService.getEffectiveOcean(pb);
        if (aOcean.isEmpty() || bOcean.isEmpty()) return null;
        double e = diff(aOcean.get("E"), bOcean.get("E"));
        double o = diff(aOcean.get("O"), bOcean.get("O"));
        double a = diff(aOcean.get("A"), bOcean.get("A"));
        double c = diff(aOcean.get("C"), bOcean.get("C"));
        double n = diff(aOcean.get("N"), bOcean.get("N"));
        double avg = (e + o + a + c + n) / 5;
        return new OceanCompatibility(avg >= 0.7 ? "高" : avg >= 0.5 ? "中" : "一般", avg);
    }

    private double diff(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return 0.5;
        double diff = Math.abs(a.doubleValue() - b.doubleValue());
        return Math.max(0, 1 - diff / 100d);
    }

    /** 工具：价值观硬筛选字段详情（Q3.3） */
    public ValueAlignment getValueAlignment(Long userAId, Long userBId) {
        UserPortrait a = userPortraitService.getPortrait(userAId);
        UserPortrait b = userPortraitService.getPortrait(userBId);
        if (a == null || b == null) return new ValueAlignment("未知", 0);
        int sexA = premaritalSexToTier(a.getPremaritalSex());
        int sexB = premaritalSexToTier(b.getPremaritalSex());
        if (sexA == 0 || sexB == 0) return new ValueAlignment("信息不足", 0);
        int diff = Math.abs(sexA - sexB);
        if (diff >= 3) return new ValueAlignment("冲突", 20);
        if (diff >= 2) return new ValueAlignment("存在分歧", 20);
        if (diff == 1) return new ValueAlignment("基本兼容", 0);
        return new ValueAlignment("兼容", 0);
    }

    private int premaritalSexToTier(String value) {
        if (value == null) return 0;
        return switch (value) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 0;
        };
    }

    private boolean same(String a, String b) {
        return a != null && Objects.equals(a, b);
    }

    public record QuestionnaireHighlight(String summary, List<String> highlights) {}
    public record OceanCompatibility(String level, double score) {
        String summary() { return level + "（" + String.format("%.0f%%", score * 100) + "）"; }
    }
    public record ValueAlignment(String status, int penalty) {
        String summary() { return status + (penalty > 0 ? "（软惩罚" + penalty + "分）" : ""); }
    }
}
