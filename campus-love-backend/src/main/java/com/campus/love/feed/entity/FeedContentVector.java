package com.campus.love.feed.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feed_content_vector")
public class FeedContentVector {
    @TableId
    private Long feedId;
    private Long userId;
    private String contentVector;  // JSON 1536维
    private String aiTags;         // JSON
    private String primaryCategory;
    private LocalDateTime createdAt;
}
