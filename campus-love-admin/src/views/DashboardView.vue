<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.userTotal }}</div>
          <div class="stat-label">用户总数</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.inviteTotal }}</div>
          <div class="stat-label">邀约总数</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats, type DashboardStats } from '@/api/adminApi'

const stats = ref<DashboardStats>({ userTotal: 0, inviteTotal: 0 })

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data.data ?? { userTotal: 0, inviteTotal: 0 }
  } catch {
    // handled by interceptor
  }
})
</script>

<style lang="scss" scoped>
.dashboard { }

.page-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; }

.stats-row { margin-bottom: 24px; }

.stat-card {
  .stat-value { font-size: 32px; font-weight: 700; color: $primary; }
  .stat-label { font-size: 14px; color: $text-muted; margin-top: 8px; }
}
</style>
