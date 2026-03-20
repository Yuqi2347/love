package com.campus.love.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_avatar")
public class UserAvatar {

    @TableId(type = IdType.INPUT)
    private Long userId;

    /** 头像二进制数据 */
    private byte[] avatarData;

    /** MIME 类型，如 image/jpeg */
    private String contentType;

    /** 文件大小（字节） */
    private Integer fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
