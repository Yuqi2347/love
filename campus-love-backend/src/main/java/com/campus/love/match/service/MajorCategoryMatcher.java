package com.campus.love.match.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 专业类目匹配服务（V2.0）
 *
 * 将专业按照教育部学科目录分类，实现7档评分：
 * - 95分：完全相同专业
 * - 80分：相同大类（如计算机 vs 软件工程）
 * - 70分：互补大类（如CS + 经济金融）
 * - 60分：相近大类（如理学 vs 工科）
 * - 50分：中性关系
 * - 40分：完全不同大类
 * - 30分：强烈不推荐组合
 *
 * @author Campus Love Team
 * @version 2.0
 */
@Slf4j
@Service
public class MajorCategoryMatcher {

    /**
     * 专业大类枚举
     */
    @Getter
    public enum MajorCategory {
        CS_IT("计算机/信息技术", Arrays.asList(
                "计算机科学与技术", "软件工程", "人工智能", "数据科学",
                "网络工程", "信息安全", "物联网工程", "数字媒体技术",
                "计算机", "软件", "编程", "开发", "算法", "前端", "后端"
        )),
        ENGINEERING("工科", Arrays.asList(
                "机械工程", "电气工程", "土木工程", "化学工程",
                "材料科学", "自动化", "电子信息工程", "通信工程",
                "测控", "车辆工程", "工业工程", "机械", "电气", "自动化"
        )),
        ECONOMICS("经济金融", Arrays.asList(
                "经济学", "金融学", "会计学", "工商管理",
                "市场营销", "国际贸易", "财务管理", "人力资源管理",
                "经济", "金融", "会计", "管理", "市场营销"
        )),
        HUMANITIES("人文社科", Arrays.asList(
                "汉语言文学", "历史学", "哲学", "社会学",
                "新闻学", "传播学", "法学", "政治学",
                "文学", "历史", "哲学", "新闻", "法学", "社工"
        )),
        SCIENCE("理学", Arrays.asList(
                "数学", "物理学", "化学", "生物学",
                "统计学", "地理学", "天文学", "心理学",
                "数学", "物理", "化学", "生物", "统计"
        )),
        MED_BIO("医学生命", Arrays.asList(
                "临床医学", "护理学", "药学", "生物技术",
                "食品科学", "医学", "护理", "药学", "生物", "医学技术"
        )),
        ART_DESIGN("艺术设计", Arrays.asList(
                "美术学", "设计学", "音乐学", "舞蹈学",
                "戏剧影视", "视觉传达", "环境设计", "产品设计",
                "美术", "设计", "音乐", "舞蹈", "表演", "导演"
        )),
        EDUCATION("教育体育", Arrays.asList(
                "教育学", "心理学", "体育学", "学前教育",
                "小学教育", "特殊教育", "体育", "教育"
        )),
        OTHER("其他", Collections.emptyList());

        private final String displayName;
        private final List<String> keywords;

        MajorCategory(String displayName, List<String> keywords) {
            this.displayName = displayName;
            this.keywords = keywords;
        }

        /**
         * 根据专业名称判断所属大类
         */
        public static MajorCategory fromMajor(String major) {
            if (major == null || major.isEmpty()) {
                return OTHER;
            }

            String normalizedMajor = major.toLowerCase().trim();

            // 优先匹配完整专业名
            for (MajorCategory category : values()) {
                if (category == OTHER) continue;
                for (String keyword : category.getKeywords()) {
                    if (normalizedMajor.contains(keyword.toLowerCase())) {
                        return category;
                    }
                }
            }

            return OTHER;
        }
    }

    /**
     * 互补专业组合
     * 这些组合在实际中有较好的互补性
     */
    private static final Set<String> COMPLEMENTARY_PAIRS = new HashSet<>() {{
        add("CS_IT-ECONOMICS");
        add("ECONOMICS-CS_IT");
        add("CS_IT-ART_DESIGN");
        add("ART_DESIGN-CS_IT");
        add("SCIENCE-ENGINEERING");
        add("ENGINEERING-SCIENCE");
        add("MED_BIO-SCIENCE");
        add("SCIENCE-MED_BIO");
        add("HUMANITIES-ECONOMICS");
        add("ECONOMICS-HUMANITIES");
    }};

    /**
     * 相近专业组合（同层次但不同领域）
     */
    private static final Set<String> NEIGHBOR_PAIRS = new HashSet<>() {{
        add("SCIENCE-ENGINEERING");
        add("ENGINEERING-SCIENCE");
        add("HUMANITIES-ART_DESIGN");
        add("ART_DESIGN-HUMANITIES");
        add("ECONOMICS-EDUCATION");
        add("EDUCATION-ECONOMICS");
    }};

    /**
     * 计算专业匹配度（7档评分）
     *
     * @param major1 专业1
     * @param major2 专业2
     * @return 匹配分数 [30, 95]
     */
    public int calculateMajorScore(String major1, String major2) {
        if (major1 == null || major2 == null) {
            return 50;  // 数据缺失时返回中等分
        }

        // 完全相同专业（忽略大小写和空格）
        String normalized1 = major1.trim().toLowerCase();
        String normalized2 = major2.trim().toLowerCase();
        if (normalized1.equals(normalized2)) {
            return 95;
        }

        MajorCategory cat1 = MajorCategory.fromMajor(major1);
        MajorCategory cat2 = MajorCategory.fromMajor(major2);

        // 相同大类
        if (cat1 == cat2 && cat1 != MajorCategory.OTHER) {
            return 80;
        }

        // 包含OTHER的情况
        if (cat1 == MajorCategory.OTHER || cat2 == MajorCategory.OTHER) {
            return 50;
        }

        String pairKey = cat1.name() + "-" + cat2.name();

        // 互补大类
        if (COMPLEMENTARY_PAIRS.contains(pairKey)) {
            return 70;
        }

        // 相近大类
        if (NEIGHBOR_PAIRS.contains(pairKey)) {
            return 60;
        }

        // 完全不同大类
        return 40;
    }

    /**
     * 判断两个专业是否相同大类
     */
    public boolean isSameCategory(String major1, String major2) {
        MajorCategory cat1 = MajorCategory.fromMajor(major1);
        MajorCategory cat2 = MajorCategory.fromMajor(major2);
        return cat1 == cat2 && cat1 != MajorCategory.OTHER;
    }

    /**
     * 判断两个专业是否互补
     */
    public boolean isComplementary(String major1, String major2) {
        MajorCategory cat1 = MajorCategory.fromMajor(major1);
        MajorCategory cat2 = MajorCategory.fromMajor(major2);
        if (cat1 == MajorCategory.OTHER || cat2 == MajorCategory.OTHER) {
            return false;
        }
        String pairKey = cat1.name() + "-" + cat2.name();
        return COMPLEMENTARY_PAIRS.contains(pairKey);
    }

    /**
     * 获取专业所属大类
     */
    public MajorCategory getMajorCategory(String major) {
        return MajorCategory.fromMajor(major);
    }

    /**
     * 获取大类显示名称
     */
    public String getCategoryDisplayName(String major) {
        return getMajorCategory(major).getDisplayName();
    }

    /**
     * 获取推荐互补专业
     *
     * @param major 当前专业
     * @param limit 限制数量
     * @return 推荐专业大类列表
     */
    public List<String> getRecommendedComplementaryMajors(String major, int limit) {
        MajorCategory category = MajorCategory.fromMajor(major);
        if (category == MajorCategory.OTHER) {
            return Collections.emptyList();
        }

        // 找出互补的大类
        return COMPLEMENTARY_PAIRS.stream()
                .filter(pair -> pair.startsWith(category.name() + "-"))
                .map(pair -> pair.substring(pair.indexOf("-") + 1))
                .map(catName -> {
                    try {
                        return MajorCategory.valueOf(catName).getDisplayName();
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    /**
     * 获取同专业大类示例
     *
     * @param major 专业名称
     * @return 同大类示例专业列表
     */
    public List<String> getSameCategoryExamples(String major) {
        MajorCategory category = MajorCategory.fromMajor(major);
        if (category == MajorCategory.OTHER) {
            return Collections.emptyList();
        }

        return category.getKeywords().stream()
                .limit(5)
                .toList();
    }
}
