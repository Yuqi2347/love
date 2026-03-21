package com.campus.love.announcement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_site_announcement")
public class SiteAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** DRAFT / PUBLISHED */
    private String status;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
