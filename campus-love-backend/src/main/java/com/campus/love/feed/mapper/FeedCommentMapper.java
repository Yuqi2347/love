package com.campus.love.feed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.feed.entity.FeedComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedCommentMapper extends BaseMapper<FeedComment> {

    /**
     * 多帖列表评论：每帖最多 {@code limit} 条（热度或时间排序），单条 SQL。
     */
    List<FeedComment> selectTopNPerPostForList(
            @Param("postIds") List<Long> postIds,
            @Param("limit") int limit,
            @Param("sortHot") boolean sortHot
    );
}
