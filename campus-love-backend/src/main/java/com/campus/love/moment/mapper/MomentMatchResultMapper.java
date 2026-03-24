package com.campus.love.moment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.moment.entity.MomentMatchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MomentMatchResultMapper extends BaseMapper<MomentMatchResult> {

    @Select("SELECT COUNT(*) FROM t_moment_match_result WHERE week_tag = #{weekTag}")
    Long countRowsByWeekTag(@Param("weekTag") String weekTag);

    @Select("""
            SELECT COUNT(DISTINCT uid) FROM (
              SELECT user_id_a AS uid FROM t_moment_match_result WHERE week_tag = #{weekTag}
              UNION ALL
              SELECT user_id_b FROM t_moment_match_result WHERE week_tag = #{weekTag}
            ) t
            """)
    Long countDistinctMatchedUsersByWeekTag(@Param("weekTag") String weekTag);

    @Select("SELECT MAX(created_at) FROM t_moment_match_result WHERE week_tag = #{weekTag}")
    LocalDateTime maxCreatedAtByWeekTag(@Param("weekTag") String weekTag);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT r.id)
            FROM t_moment_match_result r
            LEFT JOIN t_moment_match_result_content c ON c.match_result_id = r.id
            LEFT JOIN t_user u1 ON u1.id = r.user_id_a
            LEFT JOIN t_user u2 ON u2.id = r.user_id_b
            WHERE 1=1
              <if test="weekTag != null and weekTag != ''">
                AND r.week_tag = #{weekTag}
              </if>
              <if test="pool != null and pool != ''">
                AND r.pool = #{pool}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (
                  CAST(r.id AS CHAR) LIKE #{keywordLike}
                  OR CAST(r.user_id_a AS CHAR) LIKE #{keywordLike}
                  OR CAST(r.user_id_b AS CHAR) LIKE #{keywordLike}
                  OR r.week_tag LIKE #{keywordLike}
                  OR u1.nickname LIKE #{keywordLike}
                  OR u1.email LIKE #{keywordLike}
                  OR u2.nickname LIKE #{keywordLike}
                  OR u2.email LIKE #{keywordLike}
                  OR c.yuanfen_title LIKE #{keywordLike}
                )
              </if>
            </script>
            """)
    Long countAdminResultIdsFiltered(
            @Param("weekTag") String weekTag,
            @Param("pool") String pool,
            @Param("keywordLike") String keywordLike);

    @Select("""
            <script>
            SELECT DISTINCT r.id
            FROM t_moment_match_result r
            LEFT JOIN t_moment_match_result_content c ON c.match_result_id = r.id
            LEFT JOIN t_user u1 ON u1.id = r.user_id_a
            LEFT JOIN t_user u2 ON u2.id = r.user_id_b
            WHERE 1=1
              <if test="weekTag != null and weekTag != ''">
                AND r.week_tag = #{weekTag}
              </if>
              <if test="pool != null and pool != ''">
                AND r.pool = #{pool}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (
                  CAST(r.id AS CHAR) LIKE #{keywordLike}
                  OR CAST(r.user_id_a AS CHAR) LIKE #{keywordLike}
                  OR CAST(r.user_id_b AS CHAR) LIKE #{keywordLike}
                  OR r.week_tag LIKE #{keywordLike}
                  OR u1.nickname LIKE #{keywordLike}
                  OR u1.email LIKE #{keywordLike}
                  OR u2.nickname LIKE #{keywordLike}
                  OR u2.email LIKE #{keywordLike}
                  OR c.yuanfen_title LIKE #{keywordLike}
                )
              </if>
            ORDER BY r.total_score DESC, r.created_at DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Long> selectAdminResultIdsFiltered(
            @Param("weekTag") String weekTag,
            @Param("pool") String pool,
            @Param("keywordLike") String keywordLike,
            @Param("offset") long offset,
            @Param("size") long size);
}
