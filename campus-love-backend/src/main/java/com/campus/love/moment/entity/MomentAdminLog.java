package com.campus.love.moment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_moment_admin_log")
public class MomentAdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String weekTag;
    private Long operatorId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String summary;
    private String detailJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
