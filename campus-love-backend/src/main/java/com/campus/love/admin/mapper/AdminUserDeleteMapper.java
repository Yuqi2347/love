package com.campus.love.admin.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理员删除用户时，用于删除无独立 Mapper 的表数据。
 * t_user_album、t_user_match_action 在业务层无对应 Mapper，在此统一处理。
 */
@Mapper
public interface AdminUserDeleteMapper {

    /** 删除用户相册 */
    @Delete("DELETE FROM t_user_album WHERE user_id = #{userId}")
    int deleteUserAlbumByUserId(@Param("userId") Long userId);

    /** 删除用户匹配行为日志（作为操作用户或目标用户） */
    @Delete("DELETE FROM t_user_match_action WHERE user_id = #{userId} OR target_user_id = #{userId}")
    int deleteUserMatchActionByUserId(@Param("userId") Long userId);

    /** 查询用户帖子 ID（含软删，用于彻底清理） */
    @Select("SELECT id FROM t_feed_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    /** 物理删除用户朋友圈帖子（绕过 TableLogic 软删） */
    @Delete("DELETE FROM t_feed_post WHERE user_id = #{userId}")
    int deleteFeedPostsByUserId(@Param("userId") Long userId);
}
