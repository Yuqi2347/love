package com.campus.love.feed.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feed_comment")
public class FeedComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;
    private String content;
    /** 评论图片URL，逗号分隔 */
    private String images;
    private Long parentId;

    /** 被回复的用户ID（用于显示"回复 @用户名"） */
    private Long repliedUserId;

    /**
     * 业务软删：0 正常，1 已删除（对应列名仍为 deleted）。
     * Java 属性不能叫 deleted，否则会命中全局 mybatis-plus.logic-delete-field=deleted，
     * 被当成 MP 逻辑删除字段，UPDATE 为 1 后普通 SELECT 永远带 deleted=0，评论从接口里「消失」。
     */
    @TableField("deleted")
    private Integer eraseFlag;

    /** V39：点赞数 */
    private Integer likeCount;

    public static final int DELETED = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
