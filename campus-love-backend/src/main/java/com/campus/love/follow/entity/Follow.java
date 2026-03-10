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

    /**
     * 备注名 - 用户为关注的用户设置的备注
     * 优先展示备注名，点击个人主页时显示原始昵称
     */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
