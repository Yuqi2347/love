<template>
  <div class="ai-token-stats">
    <h2 class="page-title">AI 用量统计</h2>
    <p class="page-desc">全站 AI 调用统计，区分 AI 头像与其他 AI 分析两类口径</p>

    <el-radio-group v-model="range" class="range-group" @change="loadStats">
      <el-radio-button value="day">今日</el-radio-button>
      <el-radio-button value="week">近7天</el-radio-button>
      <el-radio-button value="month">近30天</el-radio-button>
    </el-radio-group>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-total">
          <div class="stat-value">{{ stats.totalTokens.toLocaleString() }}</div>
          <div class="stat-label">总 Token 消耗</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-total">
          <div class="stat-value">{{ stats.callCount }}</div>
          <div class="stat-label">总调用次数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-avatar">
          <div class="stat-value">{{ stats.avatar.tokensUsed.toLocaleString() }}</div>
          <div class="stat-label">AI 头像 Token</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-avatar">
          <div class="stat-value">{{ stats.avatar.callCount }}</div>
          <div class="stat-label">AI 头像调用次数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-analysis">
          <div class="stat-value">{{ stats.analysis.tokensUsed.toLocaleString() }}</div>
          <div class="stat-label">其他 AI 分析 Token</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="8">
        <el-card shadow="hover" class="stat-card is-analysis">
          <div class="stat-value">{{ stats.analysis.callCount }}</div>
          <div class="stat-label">其他 AI 分析调用次数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <span>每日明细</span>
      </template>
      <el-table :data="stats.dailyStats" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="totalTokens" label="总 Token" width="140">
          <template #default="{ row }">
            {{ row.totalTokens.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="总调用次数" width="120" />
        <el-table-column prop="avatarTokens" label="AI 头像 Token" width="150">
          <template #default="{ row }">
            {{ row.avatarTokens.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="avatarCallCount" label="AI 头像调用" width="130" />
        <el-table-column prop="analysisTokens" label="其他 AI 分析 Token" min-width="170">
          <template #default="{ row }">
            {{ row.analysisTokens.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="analysisCallCount" label="其他 AI 分析调用" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAiTokenStats, type AiTokenStats } from '@/api/adminApi'

const range = ref<'day' | 'week' | 'month'>('week')
const stats = ref<AiTokenStats>(createEmptyStats())

function createEmptyStats(): AiTokenStats {
  return {
    totalTokens: 0,
    callCount: 0,
    avatar: {
      tokensUsed: 0,
      callCount: 0,
    },
    analysis: {
      tokensUsed: 0,
      callCount: 0,
    },
    dailyStats: [],
  }
}

async function loadStats() {
  try {
    const res = await getAiTokenStats(range.value)
    stats.value = res.data.data ?? createEmptyStats()
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
  margin-bottom: 16px;
  border: 1px solid rgba(15, 23, 42, 0.05);

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #0f172a;
  }

  .stat-label {
    font-size: 14px;
    color: $text-muted;
    margin-top: 8px;
  }
}

.is-total .stat-value {
  color: #2563eb;
}

.is-avatar .stat-value {
  color: #0f766e;
}

.is-analysis .stat-value {
  color: #b45309;
}

.table-card {
  margin-top: 24px;
}
</style>
