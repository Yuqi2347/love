<template>
  <div class="moment-manage">
    <section class="hero-card">
      <div class="hero-main">
        <div class="hero-eyebrow">心动时刻总控</div>
        <h1>本周活动节奏和自动匹配都在这里处理</h1>
        <p>
          当前周次 {{ overview?.weekTag || '--' }}，{{ phaseText }}。
          {{ overview?.autoMatchEnabled ? `系统将在 ${formattedNextAutoMatch} 自动截止报名并执行匹配。` : '自动匹配未开启。' }}
        </p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新总览</el-button>
        <el-button plain @click="router.push('/moment/enrollments')">报名名单</el-button>
        <el-button plain @click="router.push('/moment/results')">匹配结果</el-button>
        <el-button plain @click="router.push('/moment/logs')">操作日志</el-button>
        <el-button type="primary" plain @click="router.push('/moment/config')">高级匹配配置</el-button>
      </div>
    </section>

    <el-alert
      v-if="resultMessage"
      :title="resultMessage"
      :type="resultType"
      show-icon
      closable
      class="result-alert"
      @close="resultMessage = ''"
    />

    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-label">活动状态</div>
        <div class="stat-value">{{ phaseText }}</div>
        <div class="stat-sub">{{ overview?.enrollmentOpen ? '报名开放中' : '报名已截止' }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">报名人数</div>
        <div class="stat-value">{{ overview?.participantCount ?? 0 }}</div>
        <div class="stat-sub">待匹配 {{ overview?.waitingUsers ?? 0 }} 人</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">匹配结果</div>
        <div class="stat-value">{{ overview?.matchedPairs ?? 0 }} 对</div>
        <div class="stat-sub">成功率 {{ overview?.successRate ?? 0 }}%</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">当前阈值</div>
        <div class="stat-value">{{ configForm.baseThreshold }}</div>
        <div class="stat-sub">优先匹配减免 {{ configForm.prioritizeOffset }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">下次自动匹配</div>
        <div class="stat-value small">{{ formattedNextAutoMatch }}</div>
        <div class="stat-sub">上次执行 {{ formattedLastMatch }}</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">未匹配用户</div>
        <div class="stat-value">{{ overview?.unmatchedUsers ?? 0 }}</div>
        <div class="stat-sub">已匹配 {{ overview?.matchedUsers ?? 0 }} 人</div>
      </article>
    </section>

    <section class="content-grid">
      <el-card class="panel-card action-card">
        <template #header>
          <div class="panel-header">
            <span>活动操作</span>
            <span class="panel-meta">本周 {{ overview?.weekTag || '--' }}</span>
          </div>
        </template>

        <div class="action-list">
          <div class="action-row">
            <div>
              <h3>截止报名</h3>
              <p>停止接收新的报名数据，进入待匹配状态。</p>
            </div>
            <el-button
              type="warning"
              :disabled="!overview?.canCloseEnrollment"
              :loading="actionLoading === 'close'"
              @click="handleClose"
            >
              截止报名
            </el-button>
          </div>

          <div class="action-row">
            <div>
              <h3>触发匹配</h3>
              <p>立即截止报名并执行本周匹配，结果生成后用户可查看。</p>
            </div>
            <el-button
              type="primary"
              :disabled="!overview?.canTriggerMatching"
              :loading="actionLoading === 'trigger'"
              @click="handleTrigger"
            >
              立即匹配
            </el-button>
          </div>

          <div class="action-row">
            <div>
              <h3>重新开放报名</h3>
              <p>仅在结果尚未生成时可重新开放，适合临时延长报名。</p>
            </div>
            <el-button
              type="success"
              :disabled="!overview?.canReopenEnrollment"
              :loading="actionLoading === 'reopen'"
              @click="handleReopen"
            >
              重新开放
            </el-button>
          </div>

          <div class="action-row danger">
            <div>
              <h3>重置本周活动</h3>
              <p>清除本周报名与匹配结果，重新回到可报名状态。</p>
            </div>
            <el-popconfirm
              title="确定重置本周活动吗？报名和匹配结果都会被清空。"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleReset"
            >
              <template #reference>
                <el-button
                  type="danger"
                  :disabled="!overview?.canResetWeek"
                  :loading="actionLoading === 'reset'"
                >
                  重置本周
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card automation-card">
        <template #header>
          <div class="panel-header">
            <span>自动匹配设置</span>
            <span class="panel-meta">简洁模式</span>
          </div>
        </template>

        <el-form label-position="top" class="automation-form">
          <el-form-item label="开启自动匹配">
            <el-switch v-model="configForm.autoMatchEnabled" />
          </el-form-item>

          <el-form-item label="每周执行日期">
            <el-select v-model="configForm.autoMatchDayOfWeek">
              <el-option
                v-for="item in dayOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="执行时间">
            <el-time-select
              v-model="configForm.autoMatchTime"
              start="00:00"
              step="00:30"
              end="23:30"
              format="HH:mm"
              placeholder="选择时间"
            />
          </el-form-item>

          <div class="automation-note">
            到达设定时间后，系统会先自动截止报名，再执行匹配。
          </div>

          <div class="automation-footer">
            <div class="quick-info">
              <span>基础阈值 {{ configForm.baseThreshold }}</span>
              <span>优先权叠加 {{ configForm.priorityMaxStack }} 次</span>
            </div>
            <el-button type="primary" :loading="saving" @click="saveAutomation">保存自动化设置</el-button>
          </div>
        </el-form>
      </el-card>
    </section>

    <el-card class="panel-card pool-card">
      <template #header>
        <div class="panel-header">
          <span>池分布概览</span>
          <span class="panel-meta">当前周实时统计</span>
        </div>
      </template>

      <el-table :data="overview?.poolStats || []" size="small">
        <el-table-column prop="pool" label="池" width="120">
          <template #default="{ row }">{{ poolLabel(row.pool) }}</template>
        </el-table-column>
        <el-table-column prop="participants" label="报名人数" />
        <el-table-column prop="matchedPairs" label="配对数" />
        <el-table-column prop="unmatchedUsers" label="未匹配人数" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import {
  closeMomentEnrollment,
  getMomentAdminOverview,
  getMomentMatchConfig,
  reopenMomentEnrollment,
  resetMomentWeek,
  triggerMomentMatching,
  updateMomentMatchConfig,
} from '@/api/adminApi'
import type { MomentAdminOverview, MomentMatchConfig } from '@/api/adminApi'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const actionLoading = ref('')
const resultMessage = ref('')
const resultType = ref<'success' | 'error' | 'warning'>('success')
const overview = ref<MomentAdminOverview | null>(null)
const configForm = reactive<Omit<MomentMatchConfig, 'id'>>({
  baseThreshold: 60,
  prioritizeOffset: 10,
  priorityOffset: 5,
  priorityMaxStack: 2,
  autoMatchEnabled: false,
  autoMatchDayOfWeek: 5,
  autoMatchTime: '20:00',
})

const dayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]

const phaseText = computed(() => {
  const phase = overview.value?.phase
  if (phase === 'ENROLLING') return '报名中'
  if (phase === 'WAITING_MATCH') return '待匹配'
  if (phase === 'RESULT_READY') return '结果已生成'
  return '--'
})

const formattedNextAutoMatch = computed(() => formatDateTime(overview.value?.nextAutoMatchAt, false))
const formattedLastMatch = computed(() => formatDateTime(overview.value?.lastMatchAt, true))

function poolLabel(pool: string) {
  return { MF: 'MF 异性', FF: 'FF 女生', MM: 'MM 男生' }[pool] || pool
}

function formatDateTime(value?: string | null, allowEmpty = false) {
  if (!value) {
    return allowEmpty ? '暂无执行记录' : '未开启'
  }
  const text = String(value).replace('T', ' ')
  return text.slice(0, 16)
}

function syncConfig(config?: MomentMatchConfig | null) {
  if (!config) return
  configForm.baseThreshold = config.baseThreshold
  configForm.prioritizeOffset = config.prioritizeOffset
  configForm.priorityOffset = config.priorityOffset
  configForm.priorityMaxStack = config.priorityMaxStack
  configForm.autoMatchEnabled = config.autoMatchEnabled
  configForm.autoMatchDayOfWeek = config.autoMatchDayOfWeek
  configForm.autoMatchTime = config.autoMatchTime
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, configRes] = await Promise.all([
      getMomentAdminOverview(),
      getMomentMatchConfig(),
    ])
    overview.value = overviewRes.data.data
    syncConfig(configRes.data.data)
  } catch {
    resultMessage.value = '加载心动时刻总控信息失败'
    resultType.value = 'error'
  } finally {
    loading.value = false
  }
}

async function runAction(
  key: 'close' | 'trigger' | 'reopen' | 'reset',
  action: () => Promise<unknown>,
  successMessage: string | ((payload: Record<string, unknown> | undefined) => string),
  successType: 'success' | 'warning' = 'success',
) {
  actionLoading.value = key
  try {
    const res = (await action()) as { data?: { data?: Record<string, unknown> } }
    const payload = res?.data?.data
    resultMessage.value = typeof successMessage === 'function' ? successMessage(payload) : successMessage
    resultType.value = successType
    await loadData()
  } catch {
    resultMessage.value =
      {
        close: '截止报名失败',
        trigger: '触发匹配失败',
        reopen: '重新开放失败',
        reset: '重置失败',
      }[key]
    resultType.value = 'error'
  } finally {
    actionLoading.value = ''
  }
}

async function handleClose() {
  await runAction('close', () => closeMomentEnrollment(), '已截止本周报名')
}

async function handleTrigger() {
  await runAction(
    'trigger',
    () => triggerMomentMatching(),
    (payload) => {
      const matchedPairs = typeof payload?.matchedPairs === 'number' ? payload.matchedPairs : 0
      const unmatchedUsers = typeof payload?.unmatchedUsers === 'number' ? payload.unmatchedUsers : 0
      return `匹配完成，配对 ${matchedPairs} 对，未匹配 ${unmatchedUsers} 人`
    },
  )
}

async function handleReopen() {
  await runAction('reopen', () => reopenMomentEnrollment(), '已重新开放报名')
}

async function handleReset() {
  await runAction('reset', () => resetMomentWeek(), '本周活动已重置并重新开放报名', 'warning')
}

async function saveAutomation() {
  saving.value = true
  try {
    await updateMomentMatchConfig({ ...configForm })
    resultMessage.value = '自动匹配设置已保存'
    resultType.value = 'success'
    await loadData()
  } catch {
    resultMessage.value = '保存自动匹配设置失败'
    resultType.value = 'error'
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.moment-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.18), transparent 36%),
    linear-gradient(135deg, #ffffff, #fff8ef 55%, #f8fbff);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.hero-main {
  max-width: 720px;

  .hero-eyebrow {
    margin-bottom: 10px;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #d97706;
  }

  h1 {
    margin: 0 0 8px;
    font-size: 28px;
    line-height: 1.2;
    color: #111827;
  }

  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.7;
    color: #4b5563;
  }
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.result-alert {
  margin-bottom: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);

  .stat-label {
    margin-bottom: 10px;
    font-size: 13px;
    color: #6b7280;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #111827;

    &.small {
      font-size: 19px;
      line-height: 1.35;
    }
  }

  .stat-sub {
    margin-top: 8px;
    font-size: 13px;
    color: #6b7280;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.panel-card {
  border-radius: 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
}

.panel-meta {
  font-size: 13px;
  color: #6b7280;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 0;
  border-bottom: 1px solid #eef2f7;

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }

  h3 {
    margin: 0 0 6px;
    font-size: 16px;
    color: #111827;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: #6b7280;
  }

  &.danger h3 {
    color: #b91c1c;
  }
}

.automation-form {
  display: flex;
  flex-direction: column;
}

.automation-note {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.automation-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.quick-info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 1100px) {
  .stats-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }

  .hero-actions,
  .automation-footer,
  .action-row {
    width: 100%;
  }

  .hero-actions,
  .automation-footer {
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .hero-card {
    padding: 20px;
  }

  .hero-main h1 {
    font-size: 24px;
  }

  .action-row,
  .automation-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    flex-direction: column;
  }
}
</style>
