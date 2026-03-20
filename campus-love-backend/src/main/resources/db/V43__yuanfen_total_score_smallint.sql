-- 匹配分加权后偶发 >127 会导致 TINYINT 插入失败、整行未写入，进而永远无法命中缓存
ALTER TABLE t_yuanfen_analysis_log
    MODIFY COLUMN total_score SMALLINT NULL COMMENT '触发时综合匹配分';
