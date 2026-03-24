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

    <el-alert
      v-if="dashboard?.statsNote"
      class="stats-note-alert"
      type="info"
      :closable="false"
      show-icon
    >
      <template #title>统计口径（Top-K={{ dashboard.eligibleTopK }}）</template>
      {{ dashboard.statsNote }}
    </el-alert>

    <el-card v-if="weekOverview" class="phase-card" shadow="never">
      <div class="phase-row">
        <span class="phase-label">活动阶段</span>
        <span class="phase-value">{{ weekPhaseLabel }}</span>
        <span v-if="showPipelineProgress && matchProgress" class="phase-extra">{{ progressSummary }}</span>
      </div>
    </el-card>

    <el-alert
      v-if="failureMessage"
      type="error"
      :closable="false"
      show-icon
      :title="failureMessage"
    />

    <el-row v-if="dashboard" :gutter="16" class="stats-row">
      <el-col :span="4"><el-card class="stat-card"><div class="label">周次</div><div class="value">{{ dashboard.weekTag }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">报名人数</div><div class="value">{{ dashboard.participantCount }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">{{ matchedUsersLabel }}</div><div class="value">{{ dashboard.matchedUsers }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">未匹配</div><div class="value">{{ dashboard.unmatchedUsers }}</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">成功率</div><div class="value">{{ dashboard.successRate }}%</div></el-card></el-col>
      <el-col :span="4"><el-card class="stat-card"><div class="label">当前阈值</div><div class="value">{{ dashboard.currentThreshold }}</div></el-card></el-col>
    </el-row>

    <el-row v-if="dashboard" :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card>
          <template #header>成功匹配分数趋势（折线）</template>
          <div v-if="matchedLineChart" class="line-chart">
            <svg class="line-chart-svg" viewBox="0 0 620 260" preserveAspectRatio="none">
              <line
                v-for="(tick, index) in matchedLineChart.yTicks"
                :key="`matched-y-${index}`"
                :x1="44"
                :x2="604"
                :y1="tick.y"
                :y2="tick.y"
                class="chart-grid"
              />
              <path :d="matchedLineChart.areaPath" class="chart-area primary" />
              <polyline :points="matchedLineChart.pointsAttr" class="chart-line primary" />
              <circle
                v-for="(point, index) in matchedLineChart.points"
                :key="`matched-p-${index}`"
                v-show="point.showMarker"
                :cx="point.x"
                :cy="point.y"
                r="4"
                class="chart-point primary"
              />
              <text
                v-for="(tick, index) in matchedLineChart.yTicks"
                :key="`matched-yl-${index}`"
                :x="8"
                :y="tick.y + 4"
                class="chart-y-label"
              >
                {{ tick.value }}
              </text>
              <text
                v-for="(point, index) in matchedLineChart.points"
                :key="`matched-x-${index}`"
                v-show="point.showLabel"
                :x="point.x"
                y="250"
                text-anchor="middle"
                class="chart-x-label"
              >
                {{ point.label }}
              </text>
            </svg>
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>未匹配用户最高可达分趋势（折线）</template>
          <div v-if="unmatchedLineChart" class="line-chart">
            <svg class="line-chart-svg" viewBox="0 0 620 260" preserveAspectRatio="none">
              <line
                v-for="(tick, index) in unmatchedLineChart.yTicks"
                :key="`unmatched-y-${index}`"
                :x1="44"
                :x2="604"
                :y1="tick.y"
                :y2="tick.y"
                class="chart-grid"
              />
              <path :d="unmatchedLineChart.areaPath" class="chart-area warning" />
              <polyline :points="unmatchedLineChart.pointsAttr" class="chart-line warning" />
              <circle
                v-for="(point, index) in unmatchedLineChart.points"
                :key="`unmatched-p-${index}`"
                v-show="point.showMarker"
                :cx="point.x"
                :cy="point.y"
                r="4"
                class="chart-point warning"
              />
              <text
                v-for="(tick, index) in unmatchedLineChart.yTicks"
                :key="`unmatched-yl-${index}`"
                :x="8"
                :y="tick.y + 4"
                class="chart-y-label"
              >
                {{ tick.value }}
              </text>
              <text
                v-for="(point, index) in unmatchedLineChart.points"
                :key="`unmatched-x-${index}`"
                v-show="point.showLabel"
                :x="point.x"
                y="250"
                text-anchor="middle"
                class="chart-x-label"
              >
                {{ point.label }}
              </text>
            </svg>
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
          <template #header>硬筛选统计（触发对次数）</template>
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
          <template #header>软惩罚统计（触发对次数）</template>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMomentAdminOverview,
  getMomentMatchDashboard,
  getMomentMatchProgress,
  getMomentMatchUnmatched,
  getMomentStatus,
  simulateMomentMatchDashboard,
} from '@/api/adminApi'
import type {
  MomentAdminOverview,
  MomentDashboardData,
  MomentHistogramBucket,
  MomentMatchProgressResponse,
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
const weekOverview = ref<MomentAdminOverview | null>(null)
const matchProgress = ref<MomentMatchProgressResponse | null>(null)

const currentWeekParam = computed(() => {
  const value = weekTag.value.trim()
  return value || undefined
})

const weekPhaseLabel = computed(() => {
  const p = weekOverview.value?.phase
  if (p === 'ENROLLING') return '报名中'
  if (p === 'WAITING_MATCH') return '待匹配'
  if (p === 'MATCHING') return '匹配进行中'
  if (p === 'AI_ANALYZING') return 'AI 分析中'
  if (p === 'RESULT_READY') return '可预览（待公布）'
  if (p === 'PUBLISHED') return '已公布'
  if (p === 'FAILED') return '匹配失败'
  return p || '—'
})

const showPipelineProgress = computed(() => {
  const p = weekOverview.value?.phase
  return p === 'MATCHING' || p === 'AI_ANALYZING'
})

const matchedUsersLabel = computed(() => {
  const p = weekOverview.value?.phase
  if (p === 'MATCHING' || p === 'AI_ANALYZING') {
    return '已完成池匹配'
  }
  return '匹配成功'
})

type LineChartTick = { y: number; value: string }
type LineChartPoint = {
  x: number
  y: number
  label: string
  count: number
  showLabel: boolean
  showMarker: boolean
}
type LineChartData = {
  points: LineChartPoint[]
  pointsAttr: string
  areaPath: string
  yTicks: LineChartTick[]
}

const matchedLineChart = computed(() => buildLineChartData(dashboard.value?.matchedScoreHistogram || []))
const unmatchedLineChart = computed(() => buildLineChartData(dashboard.value?.unmatchedBestScoreHistogram || []))

const progressSummary = computed(() => {
  const m = matchProgress.value
  if (!m?.matchProgress) return ''
  const mp = m.matchProgress
  const ai = m.aiProgress
  if (weekOverview.value?.phase === 'AI_ANALYZING' || mp.currentPool === 'AI') {
    return `图匹配已完成 · AI ${ai?.done ?? 0}/${ai?.total ?? 0}`
  }
  return `池 ${poolLabel(mp.currentPool || '—')} · 已处理 ${mp.processedPairs}/${mp.totalEstimatedPairs} · AI ${ai?.done ?? 0}/${ai?.total ?? 0}`
})

const failureMessage = computed(() => {
  if (weekOverview.value?.phase !== 'FAILED') return ''
  return matchProgress.value?.errorMessage || '匹配任务失败，请先查看后台日志，再决定是否重置后重试。'
})

let progressPollTimer: ReturnType<typeof setInterval> | null = null

function clearProgressPoll() {
  if (progressPollTimer != null) {
    clearInterval(progressPollTimer)
    progressPollTimer = null
  }
}

async function fetchMatchProgress() {
  try {
    const res = await getMomentMatchProgress(currentWeekParam.value)
    matchProgress.value = res.data.data ?? null
  } catch {
    /* 轮询失败保留上次 */
  }
}

watch(
  () => [weekOverview.value?.phase, currentWeekParam.value] as const,
  () => {
    clearProgressPoll()
    const p = weekOverview.value?.phase
    if (p === 'MATCHING' || p === 'AI_ANALYZING') {
      void fetchMatchProgress()
      progressPollTimer = setInterval(fetchMatchProgress, 5000)
    } else if (p === 'FAILED') {
      void fetchMatchProgress()
    } else {
      matchProgress.value = null
    }
  },
)

function poolLabel(pool: string) {
  return { MF: 'MF 异性', FF: 'FF 女生', MM: 'MM 男生', AI: 'AI 分析' }[pool] || pool
}

function buildLineChartData(buckets: MomentHistogramBucket[]): LineChartData | null {
  if (!buckets.length) return null
  const chartLeft = 44
  const chartRight = 604
  const chartTop = 20
  const chartBottom = 220
  const chartWidth = chartRight - chartLeft
  const chartHeight = chartBottom - chartTop
  const max = Math.max(...buckets.map((item) => item.count), 1)
  const stepX = buckets.length > 1 ? chartWidth / (buckets.length - 1) : 0
  const maxLabels = 12
  const labelStep = buckets.length <= 1 ? 1 : Math.max(1, Math.ceil((buckets.length - 1) / (maxLabels - 1)))
  const maxMarkers = 36
  const markerStep = buckets.length <= maxMarkers ? 1 : Math.ceil(buckets.length / maxMarkers)

  const points = buckets.map((item, index) => {
    const x = chartLeft + stepX * index
    const ratio = max === 0 ? 0 : item.count / max
    const y = chartBottom - ratio * chartHeight
    const showLabel = index === 0 || index === buckets.length - 1 || index % labelStep === 0
    const showMarker =
      buckets.length <= maxMarkers || index === 0 || index === buckets.length - 1 || index % markerStep === 0
    return { x, y, label: item.label, count: item.count, showLabel, showMarker }
  })
  const pointsAttr = points.map((point) => `${point.x},${point.y}`).join(' ')
  const areaPath = `M ${chartLeft} ${chartBottom} L ${points.map((point) => `${point.x} ${point.y}`).join(' L ')} L ${chartRight} ${chartBottom} Z`
  const yTicks: LineChartTick[] = [1, 0.75, 0.5, 0.25, 0].map((factor) => ({
    y: chartBottom - chartHeight * factor,
    value: `${Math.round(max * factor)}`,
  }))
  return { points, pointsAttr, areaPath, yTicks }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [dashboardRes, unmatchedRes, overviewRes] = await Promise.allSettled([
      getMomentMatchDashboard(currentWeekParam.value),
      getMomentMatchUnmatched(currentWeekParam.value),
      getMomentAdminOverview(currentWeekParam.value),
    ])
    if (dashboardRes.status === 'rejected') {
      ElMessage.error('加载匹配看板失败')
      return
    }
    dashboard.value = dashboardRes.value.data.data
    if (unmatchedRes.status === 'fulfilled') {
      unmatchedUsers.value = unmatchedRes.value.data.data || []
    } else {
      unmatchedUsers.value = []
      ElMessage.warning('未匹配用户明细加载失败，已显示基础看板')
    }
    if (overviewRes.status === 'fulfilled') {
      weekOverview.value = overviewRes.value.data.data ?? null
    } else {
      weekOverview.value = null
      ElMessage.warning('活动阶段信息加载失败，已显示基础看板')
    }
    if (dashboard.value) {
      simulateThreshold.value = dashboard.value.currentThreshold
    }
    simulation.value = null
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
onUnmounted(clearProgressPoll)
</script>

<style lang="scss" scoped>
.moment-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-note-alert {
  font-size: 13px;
  line-height: 1.5;
}

.phase-card {
  padding: 12px 16px;
  border-radius: 12px;

  .phase-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 16px;
    font-size: 14px;
  }

  .phase-label {
    color: #6b7280;
  }

  .phase-value {
    font-weight: 600;
    color: #111827;
  }

  .phase-extra {
    color: #4b5563;
    font-size: 13px;
  }
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

.line-chart {
  width: 100%;
  height: 260px;
}

.line-chart-svg {
  width: 100%;
  height: 260px;
}

.chart-grid {
  stroke: #edf1f7;
  stroke-width: 1;
}

.chart-line {
  fill: none;
  stroke-width: 3;

  &.primary {
    stroke: #3b82f6;
  }

  &.warning {
    stroke: #f59e0b;
  }
}

.chart-area {
  stroke: none;

  &.primary {
    fill: rgba(59, 130, 246, 0.12);
  }

  &.warning {
    fill: rgba(245, 158, 11, 0.14);
  }
}

.chart-point {
  stroke: #fff;
  stroke-width: 2;

  &.primary {
    fill: #3b82f6;
  }

  &.warning {
    fill: #f59e0b;
  }
}

.chart-x-label,
.chart-y-label {
  fill: #7c869b;
  font-size: 11px;
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
