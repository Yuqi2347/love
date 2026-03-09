<template>
  <div class="ai-token-stats">
    <h2 class="page-title">AI Token 消耗统计</h2>
    <p class="page-desc">缘分解析功能的通义千问 Token 消耗记录与统计</p>

    <el-radio-group v-model="range" class="range-group" @change="loadStats">
      <el-radio-button value="day">今日</el-radio-button>
      <el-radio-button value="week">近7天</el-radio-button>
      <el-radio-button value="month">近30天</el-radio-button>
    </el-radio-group>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.totalTokens.toLocaleString() }}</div>
          <div class="stat-label">总 Token 消耗</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.callCount }}</div>
          <div class="stat-label">调用次数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <span>每日明细</span>
      </template>
      <el-table :data="stats.dailyStats" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="tokensUsed" label="Token 消耗" width="140">
          <template #default="{ row }">
            {{ row.tokensUsed.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAiTokenStats, type AiTokenStats } from '@/api/adminApi'

const range = ref<'day' | 'week' | 'month'>('week')
const stats = ref<AiTokenStats>({
  totalTokens: 0,
  callCount: 0,
  dailyStats: [],
})

async function loadStats() {
  try {
    const res = await getAiTokenStats(range.value)
    stats.value = res.data.data ?? { totalTokens: 0, callCount: 0, dailyStats: [] }
  } catch {
    // handled by interceptor
  }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.ai-token-stats {
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 14px;
  color: $text-muted;
  margin-bottom: 24px;
}

.range-group {
  margin-bottom: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: $primary;
  }
  .stat-label {
    font-size: 14px;
    color: $text-muted;
    margin-top: 8px;
  }
}

.table-card {
  margin-top: 24px;
}
</style>
