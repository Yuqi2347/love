package com.campus.love.ai.agent;

import com.campus.love.ai.service.AiService;
import com.campus.love.ai.service.YuanFenService;
import com.campus.love.profile.entity.UserAiProfile;
import com.campus.love.profile.mapper.UserAiProfileMapper;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final UserAiProfileMapper userAiProfileMapper;
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
        return String.format("%s | %s%s | %s | 兴趣:%s",
                u.getNickname() != null ? u.getNickname() : "匿名",
                u.getSchool() != null ? u.getSchool() : "",
                u.getGrade() != null ? u.getGrade() : "",
                u.getMajor() != null ? u.getMajor() : "未填",
                u.getInterests() != null ? u.getInterests() : "未填");
    }

    /** 工具：获取问卷最显著差异和契合点 */
    public QuestionnaireHighlight getQuestionnaireHighlight(Long userAId, Long userBId) {
        // TODO: 接入 t_moment_profile 32题问卷
        return new QuestionnaireHighlight("性格与价值观较为契合", List.of("社交风格相近", "生活节奏相似"));
    }

    /** 工具：OCEAN契合分析（仅当双方均有真实OCEAN时） */
    public OceanCompatibility getOceanCompatibility(Long userAId, Long userBId) {
        UserAiProfile pa = userAiProfileMapper.selectById(userAId);
        UserAiProfile pb = userAiProfileMapper.selectById(userBId);
        if (pa == null || pb == null || !Boolean.TRUE.equals(pa.getHasRealOcean()) || !Boolean.TRUE.equals(pb.getHasRealOcean())) {
            return null;
        }
        // 简化：五维差值加权
        double e = diff(pa.getOceanEShort(), pb.getOceanEShort());
        double o = diff(pa.getOceanOShort(), pb.getOceanOShort());
        double a = diff(pa.getOceanAShort(), pb.getOceanAShort());
        double c = diff(pa.getOceanCShort(), pb.getOceanCShort());
        double n = diff(pa.getOceanNShort(), pb.getOceanNShort());
        double avg = (e + o + a + c + n) / 5;
        return new OceanCompatibility(avg >= 0.7 ? "高" : avg >= 0.5 ? "中" : "一般", avg);
    }

    private double diff(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null || b == null) return 0.5;
        double diff = Math.abs(a.doubleValue() - b.doubleValue());
        return Math.max(0, 1 - diff / 9);
    }

    /** 工具：价值观硬筛选字段详情（Q3.3） */
    public ValueAlignment getValueAlignment(Long userAId, Long userBId) {
        // TODO: 接入 t_moment_profile.premarital_sex
        return new ValueAlignment("兼容", 0);
    }

    public record QuestionnaireHighlight(String summary, List<String> highlights) {}
    public record OceanCompatibility(String level, double score) {
        String summary() { return level + "（" + String.format("%.0f%%", score * 100) + "）"; }
    }
    public record ValueAlignment(String status, int penalty) {
        String summary() { return status + (penalty > 0 ? "（软惩罚" + penalty + "分）" : ""); }
    }
}
