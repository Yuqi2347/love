package com.campus.love.common.constants;

/**
 * embedding 迁移预警阈值
 * 用户量超 USER_COUNT_THRESHOLD 或 ANN 查询 P99 超 P99_MS_THRESHOLD 时需迁移至 pgvector/Milvus
 */
public final class EmbeddingMigrationConstants {
    /** 用户量阈值：超过需迁移 */
    public static final int USER_COUNT_THRESHOLD = 5000;
    /** ANN 查询 P99 延迟阈值(ms)：超过需迁移 */
    public static final int P99_MS_THRESHOLD = 500;

    private EmbeddingMigrationConstants() {}
}
