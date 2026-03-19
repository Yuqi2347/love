<template>
  <div class="moment-dashboard">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="weekTag" placeholder="活动周，例如 2026-W11" clearable class="week-input" />
          <el-button type="primary" :loading="loading" @click="loadDashboard">刷新看板</el-button>
        </div>
        <el-button :disabled="!unmatchedUsers.length" @click="exportUnmatched">导出未匹配明细</el-button>
      </div>
    </el-card>

    <el-row v-if="dashboard" :gutter="16" class="stats-row">
      <el-col :span="4"><el-card class="stat-card"><div class="label">周次</div><div class="value">{{ dashboard.weekTag }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">报名人数</div><div class="value">{{ dashboard.participantCount }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">匹配成功</div><div class="value">{{ dashboard.matchedUsers }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">未匹配</div><div class="value">{{ dashboard.unmatchedUsers }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">成功率</div><div class="value">{{ dashboard.successRate }}%</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">当前阈值</div><div class="value">{{ dashboard.currentThreshold }}</div></el-card></el-col>
    </el-row>

    <el-row v-if="dashboard" :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card>
          <template #header>成功匹配分数分布</template>
          <div v-if="dashboard.matchedScoreHistogram.length" class="histogram">
            <div v-for="item in dashboard.matchedScoreHistogram" :key="item.label" class="histogram-row">
              <span class="bucket-label">{{ item.label }}</span>
              <div class="bar-track">
                <div class="bar-fill primary" :style="{ width: `${barWidth(item.count, dashboard.matchedScoreHistogram)}%` }" />
              </div>
              <span class="bucket-count">{{ item.count }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>未匹配用户最高可达分</template>
          <div v-if="dashboard.unmatchedBestScoreHistogram.length" class="histogram">
            <div v-for="item in dashboard.unmatchedBestScoreHistogram" :key="item.label" class="histogram-row">
              <span class="bucket-label">{{ item.label }}</span>
              <div class="bar-track">
                <div class="bar-fill warning" :style="{ width: `${barWidth(item.count, dashboard.unmatchedBestScoreHistogram)}%` }" />
              </div>
              <span class="bucket-count">{{ item.count }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
          <div class="sub-metric">被阈值过滤的配对数：{{ dashboard.filteredPairCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="dashboard" :gutter="16" class="content-row">
      <el-col :span="10">
        <el-card>
          <template #header>阈值模拟器</template>
          <div class="simulator">
            <el-input-number v-model="simulateThreshold" :min="0" :max="100" />
            <el-button type="primary" :loading="simulateLoading" @click="simulateThresholdNow">模拟</el-button>
          </div>
          <el-alert
            v-if="simulation"
            class="simulation-alert"
            type="info"
            :closable="false"
            show-icon
            :title="`预计配对 ${simulation.matchedPairs} 对，成功率 ${simulation.successRate}%，较当前 ${simulation.deltaPairs >= 0 ? '+' : ''}${simulation.deltaPairs} 对`"
          />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <template #header>池分布</template>
          <el-table :data="dashboard.poolStats" size="small">
            <el-table-column prop="pool" label="池" width="100">
              <template #default="{ row }">{{ poolLabel(row.pool) }}</template>
            </el-table-column>
            <el-table-column prop="participants" label="报名" />
            <el-table-column prop="matchedPairs" label="配对数" />
            <el-table-column prop="unmatchedUsers" label="未匹配" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="dashboard" :gutter="16" class="content-row">
      <el-col :span="8">
        <el-card>
          <template #header>硬筛选统计</template>
          <div v-if="dashboard.hardFilterStats.length" class="reason-list">
            <div v-for="item in dashboard.hardFilterStats" :key="item.reason" class="reason-row">
              <span>{{ item.reason }}</span>
              <el-tag type="danger">{{ item.count }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无触发" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>软惩罚统计</template>
          <div v-if="dashboard.softPenaltyStats.length" class="reason-list">
            <div v-for="item in dashboard.softPenaltyStats" :key="item.reason" class="reason-row">
              <span>{{ item.reason }}</span>
              <el-tag type="warning">{{ item.count }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无触发" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>未匹配原因统计</template>
          <div v-if="dashboard.unmatchedReasonStats.length" class="reason-list">
            <div v-for="item in dashboard.unmatchedReasonStats" :key="item.reason" class="reason-row">
              <span>{{ item.reason }}</span>
              <el-tag>{{ item.count }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="72" />
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="dashboard" class="content-row">
      <template #header>未匹配用户明细</template>
      <el-table :data="unmatchedUsers" size="small">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="pool" label="池" width="100">
          <template #default="{ row }">{{ poolLabel(row.pool) }}</template>
        </el-table-column>
        <el-table-column prop="highestAvailableScore" label="最高可达分" width="120">
          <template #default="{ row }">{{ row.highestAvailableScore ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="未匹配原因" min-width="180" />
        <el-table-column prop="priorityCount" label="优先权" width="90" />
        <el-table-column prop="prioritizeMatching" label="优先匹配" width="100">
          <template #default="{ row }">{{ row.prioritizeMatching ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="dashboard" class="content-row">
      <template #header>阈值过滤配对示例</template>
      <el-table :data="dashboard.filteredPairSamples" size="small">
        <el-table-column prop="pool" label="池" width="100">
          <template #default="{ row }">{{ poolLabel(row.pool) }}</template>
        </el-table-column>
        <el-table-column prop="userIdA" label="用户A" width="110" />
        <el-table-column prop="userIdB" label="用户B" width="110" />
        <el-table-column prop="score" label="得分" width="100" />
        <el-table-column prop="thresholdRequired" label="所需阈值" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMomentMatchDashboard,
  getMomentMatchUnmatched,
  getMomentStatus,
  simulateMomentMatchDashboard,
} from '@/api/adminApi'
import type {
  MomentDashboardData,
  MomentHistogramBucket,
  MomentSimulationResponse,
  MomentUnmatchedUser,
} from '@/api/adminApi'

const loading = ref(false)
const simulateLoading = ref(false)
const weekTag = ref('')
const simulateThreshold = ref(60)
const dashboard = ref<MomentDashboardData | null>(null)
const unmatchedUsers = ref<MomentUnmatchedUser[]>([])
const simulation = ref<MomentSimulationResponse | null>(null)

const currentWeekParam = computed(() => {
  const value = weekTag.value.trim()
  return value || undefined
})

function poolLabel(pool: string) {
  return { MF: 'MF 异性', FF: 'FF 女生', MM: 'MM 男生' }[pool] || pool
}

function barWidth(count: number, buckets: MomentHistogramBucket[]) {
  const max = Math.max(...buckets.map((item) => item.count), 1)
  return Math.max(8, Math.round((count / max) * 100))
}

async function loadDashboard() {
  loading.value = true
  try {
    const [dashboardRes, unmatchedRes] = await Promise.all([
      getMomentMatchDashboard(currentWeekParam.value),
      getMomentMatchUnmatched(currentWeekParam.value),
    ])
    dashboard.value = dashboardRes.data.data
    unmatchedUsers.value = unmatchedRes.data.data || []
    if (dashboard.value) {
      simulateThreshold.value = dashboard.value.currentThreshold
    }
    simulation.value = null
  } catch {
    ElMessage.error('加载匹配看板失败')
  } finally {
    loading.value = false
  }
}

async function simulateThresholdNow() {
  if (!dashboard.value) return
  simulateLoading.value = true
  try {
    const res = await simulateMomentMatchDashboard({
      weekTag: dashboard.value.weekTag,
      threshold: simulateThreshold.value,
    })
    simulation.value = res.data.data
  } catch {
    ElMessage.error('阈值模拟失败')
  } finally {
    simulateLoading.value = false
  }
}

function exportUnmatched() {
  if (!unmatchedUsers.value.length) return
  const header = ['userId', 'nickname', 'pool', 'highestAvailableScore', 'reason', 'priorityCount', 'prioritizeMatching']
  const rows = unmatchedUsers.value.map((item) => [
    item.userId,
    item.nickname || '',
    item.pool,
    item.highestAvailableScore ?? '',
    item.reason,
    item.priorityCount,
    item.prioritizeMatching ? 'true' : 'false',
  ])
  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `moment-unmatched-${dashboard.value?.weekTag || 'week'}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(async () => {
  try {
    const statusRes = await getMomentStatus()
    weekTag.value = statusRes.data.data?.currentWeek || ''
  } catch {
    // ignore
  }
  await loadDashboard()
})
</script>

<style lang="scss" scoped>
.moment-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .trigger-alert {
    margin-top: 16px;
  }

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .week-input {
    width: 220px;
  }
}

.stats-row {
  margin: 0;
}

.stat-card {
  .label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }

  .value {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
  }
}

.content-row {
  margin-top: 0;
}

.histogram {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.histogram-row {
  display: grid;
  grid-template-columns: 72px 1fr 40px;
  align-items: center;
  gap: 12px;
}

.bucket-label,
.bucket-count {
  font-size: 13px;
  color: #606266;
}

.bar-track {
  height: 10px;
  background: #f2f3f5;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;

  &.primary {
    background: linear-gradient(90deg, #7c4dff, #409eff);
  }

  &.warning {
    background: linear-gradient(90deg, #f59e0b, #f97316);
  }
}

.sub-metric {
  margin-top: 16px;
  font-size: 13px;
  color: #606266;
}

.simulator {
  display: flex;
  align-items: center;
  gap: 12px;
}

.simulation-alert {
  margin-top: 16px;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reason-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: #303133;
}
</style>
