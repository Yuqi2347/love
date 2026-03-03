package com.campus.love.follow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_follow")
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long followerId;
    private Long followingId;
    private Boolean isMutual;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
