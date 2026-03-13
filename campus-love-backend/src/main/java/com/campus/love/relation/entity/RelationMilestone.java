package com.campus.love.relation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_relation_milestone")
public class RelationMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    @com.baomidou.mybatisplus.annotation.TableField("user_id_a")
    private Long userIdA;
    @com.baomidou.mybatisplus.annotation.TableField("user_id_b")
    private Long userIdB;
    private String milestoneType;
    private LocalDateTime notifiedAt;
}
