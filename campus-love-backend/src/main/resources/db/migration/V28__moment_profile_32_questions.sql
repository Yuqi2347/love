-- V28: 心动一刻问卷对齐文档 2026-03-12 版（3步骤26题+）
-- 数据库已清空，直接添加新字段

ALTER TABLE t_moment_profile
    ADD COLUMN appearance_score DECIMAL(3,1) DEFAULT NULL COMMENT '1.1 颜值自评',
    ADD COLUMN personality_base CHAR(1) DEFAULT NULL COMMENT '1.4 性格底色 A/B/C',
    ADD COLUMN campus_focus CHAR(1) DEFAULT NULL COMMENT '1.5 校园生活重心 A/B/C/D',
    ADD COLUMN emotion_style CHAR(1) DEFAULT NULL COMMENT '1.6 情绪表达 A/B/C',
    ADD COLUMN grade_range_preference CHAR(1) DEFAULT NULL COMMENT '2.4 年级范围 A/B/C/D',
    ADD COLUMN career_ambition_pref CHAR(1) DEFAULT NULL COMMENT '2.7 事业心偏好 A/B/C/D',
    ADD COLUMN honesty_level CHAR(1) DEFAULT NULL COMMENT '3.1 坦诚度 A/B/C',
    ADD COLUMN premarital_sex CHAR(1) DEFAULT NULL COMMENT '3.3 婚前性行为 A/B/C/D',
    ADD COLUMN core_value CHAR(1) DEFAULT NULL COMMENT '3.4 核心价值 A/B/C/D',
    ADD COLUMN conflict_style CHAR(1) DEFAULT NULL COMMENT '3.5 矛盾解决 A/B/C/D',
    ADD COLUMN social_boundary CHAR(1) DEFAULT NULL COMMENT '3.6 社交边界 A/B/C',
    ADD COLUMN campus_love_plan CHAR(1) DEFAULT NULL COMMENT '3.8 校园恋爱规划 A/B/C',
    ADD COLUMN idol_role CHAR(1) DEFAULT NULL COMMENT '3.9 偶像角色认知 A/B/C/D',
    ADD COLUMN temptation_response CHAR(1) DEFAULT NULL COMMENT '3.10 面对诱惑 A/B/C/D',
    ADD COLUMN reality_condition CHAR(1) DEFAULT NULL COMMENT '3.11 现实条件 A/B/C/D',
    ADD COLUMN human_nature_view CHAR(1) DEFAULT NULL COMMENT '3.12 人性观 A/B/C/D',
    ADD COLUMN breakup_view CHAR(1) DEFAULT NULL COMMENT '3.13 分手观 A/B/C/D',
    ADD COLUMN career_love_conflict CHAR(1) DEFAULT NULL COMMENT '3.14 事业爱情冲突 A/B/C/D',
    ADD COLUMN emotion_priority CHAR(1) DEFAULT NULL COMMENT '3.15 情感排序 A/B/C',
    ADD COLUMN life_goal_priority CHAR(1) DEFAULT NULL COMMENT '3.16 人生目标优先级 A/B/C';
