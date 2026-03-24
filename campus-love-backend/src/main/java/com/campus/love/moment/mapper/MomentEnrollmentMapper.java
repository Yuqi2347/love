package com.campus.love.moment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.moment.entity.MomentEnrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MomentEnrollmentMapper extends BaseMapper<MomentEnrollment> {

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_moment_enrollment WHERE week_tag = #{weekTag}")
    Long countDistinctUsersByWeekTag(@Param("weekTag") String weekTag);

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_moment_enrollment WHERE week_tag = #{weekTag} AND status = 'WAITING'")
    Long countDistinctWaitingByWeekTag(@Param("weekTag") String weekTag);

    /**
     * 与 {@link com.campus.love.moment.service.MomentAdminService#getOverview} 中集合差一致：
     * 已报名 ∩ 非「待匹配」∩ 未出现在任一匹配结果中。
     */
    @Select("""
            SELECT COUNT(DISTINCT e.user_id) FROM t_moment_enrollment e
            WHERE e.week_tag = #{weekTag}
            AND NOT EXISTS (
              SELECT 1 FROM t_moment_enrollment w
              WHERE w.week_tag = e.week_tag AND w.user_id = e.user_id AND w.status = 'WAITING'
            )
            AND NOT EXISTS (
              SELECT 1 FROM t_moment_match_result r
              WHERE r.week_tag = e.week_tag AND (r.user_id_a = e.user_id OR r.user_id_b = e.user_id)
            )
            """)
    Long countDistinctUnmatchedByWeekTag(@Param("weekTag") String weekTag);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT CONCAT(e.week_tag, '#', e.user_id))
            FROM t_moment_enrollment e
            LEFT JOIN t_user u ON u.id = e.user_id
            WHERE 1=1
              <if test="weekTag != null and weekTag != ''">
                AND e.week_tag = #{weekTag}
              </if>
              <if test="pool != null and pool != ''">
                AND e.pool = #{pool}
              </if>
              <if test="gender != null">
                AND u.gender = #{gender}
              </if>
              <if test="status != null and status != ''">
                AND e.status = #{status}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (
                  CAST(e.user_id AS CHAR) LIKE #{keywordLike}
                  OR e.week_tag LIKE #{keywordLike}
                  OR u.nickname LIKE #{keywordLike}
                  OR u.email LIKE #{keywordLike}
                  OR u.school LIKE #{keywordLike}
                  OR u.major LIKE #{keywordLike}
                  OR u.grade LIKE #{keywordLike}
                )
              </if>
            </script>
            """)
    Long countAdminEnrollmentGroups(
            @Param("weekTag") String weekTag,
            @Param("pool") String pool,
            @Param("gender") Integer gender,
            @Param("status") String status,
            @Param("keywordLike") String keywordLike);

    @Select("""
            <script>
            SELECT
              e.week_tag AS weekTag,
              e.user_id AS userId,
              COALESCE(NULLIF(u.nickname, ''), NULLIF(u.email, ''), CONCAT('用户', e.user_id)) AS nickname,
              u.gender AS gender,
              u.school AS school,
              u.major AS major,
              u.grade AS grade,
              GROUP_CONCAT(DISTINCT e.pool ORDER BY FIELD(e.pool, 'MF', 'FF', 'MM') SEPARATOR ',') AS pools,
              SUBSTRING_INDEX(GROUP_CONCAT(e.status ORDER BY e.created_at DESC SEPARATOR ','), ',', 1) AS status,
              COALESCE(u.moment_priority_count, 0) AS priorityCount,
              MAX(e.created_at) AS createdAt
            FROM t_moment_enrollment e
            LEFT JOIN t_user u ON u.id = e.user_id
            WHERE 1=1
              <if test="weekTag != null and weekTag != ''">
                AND e.week_tag = #{weekTag}
              </if>
              <if test="pool != null and pool != ''">
                AND e.pool = #{pool}
              </if>
              <if test="gender != null">
                AND u.gender = #{gender}
              </if>
              <if test="status != null and status != ''">
                AND e.status = #{status}
              </if>
              <if test="keywordLike != null and keywordLike != ''">
                AND (
                  CAST(e.user_id AS CHAR) LIKE #{keywordLike}
                  OR e.week_tag LIKE #{keywordLike}
                  OR u.nickname LIKE #{keywordLike}
                  OR u.email LIKE #{keywordLike}
                  OR u.school LIKE #{keywordLike}
                  OR u.major LIKE #{keywordLike}
                  OR u.grade LIKE #{keywordLike}
                )
              </if>
            GROUP BY e.week_tag, e.user_id, u.nickname, u.email, u.gender, u.school, u.major, u.grade, u.moment_priority_count
            ORDER BY createdAt DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> selectAdminEnrollmentGroups(
            @Param("weekTag") String weekTag,
            @Param("pool") String pool,
            @Param("gender") Integer gender,
            @Param("status") String status,
            @Param("keywordLike") String keywordLike,
            @Param("offset") long offset,
            @Param("size") long size);
}
