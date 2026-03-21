package com.campus.love.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.announcement.entity.SiteAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SiteAnnouncementMapper extends BaseMapper<SiteAnnouncement> {

    @Select("""
            SELECT a.* FROM t_site_announcement a
            WHERE a.status = 'PUBLISHED'
              AND a.published_at IS NOT NULL
              AND a.valid_from <= #{now}
              AND a.valid_until >= #{now}
              AND NOT EXISTS (
                  SELECT 1 FROM t_user_announcement_read r
                  WHERE r.user_id = #{userId} AND r.announcement_id = a.id
              )
            ORDER BY a.published_at DESC, a.id DESC
            LIMIT #{limit}
            """)
    List<SiteAnnouncement> selectUnreadForUser(@Param("userId") Long userId,
                                                 @Param("now") LocalDateTime now,
                                                 @Param("limit") int limit);
}
