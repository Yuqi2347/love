package com.campus.love.announcement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_announcement_read")
public class UserAnnouncementRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long announcementId;

    private LocalDateTime readAt;
}
