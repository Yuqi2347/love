package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兴趣标签元数据
 */
@Data
@TableName("t_interest_tag_meta")
public class InterestTagMeta {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagCode;
    private String tagName;
    private String dimension;
    private String signals;  // JSON
    private LocalDateTime createdAt;
}
