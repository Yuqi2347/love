package com.campus.love.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminStatsMapper {

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_activity_log WHERE created_at >= CURDATE()")
    long countActiveUsersToday();

    @Select("SELECT COUNT(DISTINCT user_id) FROM t_activity_log WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)")
    long countActiveUsers7d();

    @Select("SELECT COUNT(*) FROM t_user WHERE profile_complete = 1 AND deleted_at IS NULL")
    long countProfileComplete();

    @Select("SELECT COUNT(*) FROM t_feed_post WHERE deleted = 0")
    long countFeedTotal();

    @Select("SELECT COUNT(*) FROM t_user WHERE created_at >= CURDATE() AND deleted_at IS NULL")
    long countNewUsersToday();

    @Select("SELECT COUNT(*) FROM t_user_embedding")
    long countEmbedding();
}
