package com.campus.love.tracking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.tracking.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {

    @Select("SELECT DISTINCT target_id FROM t_user_behavior_log " +
            "WHERE user_id = #{userId} AND behavior_type = #{behaviorType} " +
            "AND created_at >= #{since} LIMIT #{limit}")
    List<Long> selectViewedTargetIds(@Param("userId") Long userId,
                                     @Param("behaviorType") String behaviorType,
                                     @Param("since") LocalDateTime since,
                                     @Param("limit") int limit);
}
