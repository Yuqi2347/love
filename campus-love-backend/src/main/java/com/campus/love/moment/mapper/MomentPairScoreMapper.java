package com.campus.love.moment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.moment.entity.MomentPairScore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MomentPairScoreMapper extends BaseMapper<MomentPairScore> {

    @Insert({
            "<script>",
            "INSERT INTO t_moment_pair_score ",
            "(week_tag, pool, user_id_a, user_id_b, score, hard_filter_passed, included_by_threshold, matched) ",
            "VALUES ",
            "<foreach collection='rows' item='row' separator=','>",
            "(",
            "#{row.weekTag}, ",
            "#{row.pool}, ",
            "#{row.userIdA}, ",
            "#{row.userIdB}, ",
            "#{row.score}, ",
            "#{row.hardFilterPassed}, ",
            "#{row.includedByThreshold}, ",
            "#{row.matched}",
            ")",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("rows") List<MomentPairScore> rows);
}
