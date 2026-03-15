<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">仪表盘</h2>
      <el-button type="primary" :loading="loading" @click="fetchStats">刷新</el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.userTotal }}</div>
          <div class="stat-label">用户总数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.activeUsersToday }}</div>
          <div class="stat-label">今日活跃</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.activeUsers7d }}</div>
          <div class="stat-label">7日活跃</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.profileCompleteCount }}</div>
          <div class="stat-label">完善资料</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.inviteTotal }}</div>
          <div class="stat-label">邀约总数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.feedTotal }}</div>
          <div class="stat-label">帖子总数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.newUsersToday }}</div>
          <div class="stat-label">今日新增</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.embeddingCount }}</div>
          <div class="stat-label">Embedding 数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="embeddingMigrationWarning"
      type="warning"
      :closable="false"
      show-icon
      class="migration-alert"
    >
      <template #title>Embedding 迁移预警</template>
      用户数或 Embedding 数已超过 5000，建议迁移至 pgvector 或 Milvus。详见表注释。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getDashboardStats, type DashboardStats } from '@/api/adminApi'

const EMBEDDING_THRESHOLD = 5000

const loading = ref(false)
const stats = ref<DashboardStats>({
  userTotal: 0,
  inviteTotal: 0,
  feedTotal: 0,
  activeUsersToday: 0,
  activeUsers7d: 0,
  profileCompleteCount: 0,
  newUsersToday: 0,
  embeddingCount: 0,
})

const embeddingMigrationWarning = computed(() => {
  return stats.value.userTotal > EMBEDDING_THRESHOLD || stats.value.embeddingCount > EMBEDDING_THRESHOLD
})

async function fetchStats() {
  loading.value = true
  try {
    const res = await getDashboardStats()
    const data = res.data.data
    if (data) {
      stats.value = {
        userTotal: data.userTotal ?? 0,
        inviteTotal: data.inviteTotal ?? 0,
        feedTotal: data.feedTotal ?? 0,
        activeUsersToday: data.activeUsersToday ?? 0,
        activeUsers7d: data.activeUsers7d ?? 0,
        profileCompleteCount: data.profileCompleteCount ?? 0,
        newUsersToday: data.newUsersToday ?? 0,
        embeddingCount: data.embeddingCount ?? 0,
      }
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)

// 定时刷新（每 60 秒）
const interval = setInterval(fetchStats, 60000)
onUnmounted(() => clearInterval(interval))
</script>

<style lang="scss" scoped>
.dashboard { }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title { font-size: 20px; font-weight: 700; margin: 0; }

.stats-row { margin-bottom: 24px; }

.stat-card {
  margin-bottom: 20px;
  .stat-value { font-size: 28px; font-weight: 700; color: var(--el-color-primary); }
  .stat-label { font-size: 14px; color: var(--el-text-color-secondary); margin-top: 8px; }
}

.migration-alert { margin-top: 20px; }
</style>
