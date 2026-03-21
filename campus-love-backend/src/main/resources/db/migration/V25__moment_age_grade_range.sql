-- V25: 心动问卷年龄/年级范围字段（双滑块）
ALTER TABLE t_moment_profile
    ADD COLUMN age_preference_min INT DEFAULT -10 COMMENT '年龄偏好下限（相对年龄，-10到10）',
    ADD COLUMN age_preference_max INT DEFAULT 10 COMMENT '年龄偏好上限',
    ADD COLUMN grade_range_min INT DEFAULT 1 COMMENT '年级范围下限（1大一-11博四）',
    ADD COLUMN grade_range_max INT DEFAULT 11 COMMENT '年级范围上限';
