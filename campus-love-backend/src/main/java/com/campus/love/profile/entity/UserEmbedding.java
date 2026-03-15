package com.campus.love.profile.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * embedding 暂用 JSON 存储
 * 技术债：用户量超5000或ANN查询P99超500ms时迁移至pgvector或Milvus
 */
@Data
@TableName("t_user_embedding")
public class UserEmbedding {
    @TableId
    private Long userId;
    private String embedding;  // JSON 1536维
    private LocalDateTime updatedAt;
}
