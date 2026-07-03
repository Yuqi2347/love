<template>
  <div class="moment-page">
    <el-alert
      v-if="serverWeekTag"
      type="info"
      show-icon
      :closable="false"
      class="page-tip"
      :title="`服务端当前活动周：${serverWeekTag}（周次留空时按此周查询）`"
    >
      <template #default>
        <p class="tip-lines">
          活动周按<strong>北京时间</strong>、以<strong>周日</strong>为新一周起点编号；若昨天报名的用户今天「不见了」，请先核对是否已换周，并在上方输入<strong>昨天的周次</strong>再查。
        </p>
        <p class="tip-lines">
          <strong>自动匹配</strong>由「匹配配置」里的开关 + 周几/时刻决定，到点会截止报名并跑匹配，<strong>不会删除</strong>报名记录，只会把状态改为已匹配/未匹配。
          <strong>用户能看见缘分详情</strong>要等管理员「公布结果」或开启<strong>自动公布</strong>（可与周五倒计时一致，也可单独配置）。
        </p>
        <p v-if="autoMatchLine" class="tip-lines muted">{{ autoMatchLine }}</p>
      </template>
    </el-alert>
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-main">
          <el-input v-model="filters.weekTag" placeholder="活动周，例如 2026-W12" clearable class="field-week" />
          <el-select v-model="filters.pool" placeholder="匹配池" clearable class="field-short">
            <el-option label="MF 异性" value="MF" />
            <el-option label="FF 女生" value="FF" />
            <el-option label="MM 男生" value="MM" />
          </el-select>
          <el-select v-model="filters.gender" placeholder="性别" clearable class="field-short">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
          <el-select v-model="filters.status" placeholder="状态" clearable class="field-short">
            <el-option label="待匹配" value="WAITING" />
            <el-option label="已匹配" value="MATCHED" />
            <el-option label="未匹配" value="UNMATCHED" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="搜索用户ID / 昵称 / 学校 / 专业" clearable class="field-keyword" />
        </div>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="panel-header">
          <span>报名名单</span>
          <span class="panel-meta">支持移除待匹配中的报名用户</span>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="weekTag" label="周次" width="120" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="性别" width="90">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="学校 / 专业" min-width="220">
          <template #default="{ row }">
            <div class="stack-text">
              <span>{{ row.school || '--' }}</span>
              <span>{{ row.major || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column label="报名池" min-width="160">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="pool in row.pools" :key="pool" size="small" effect="plain">{{ poolLabel(pool) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先匹配" width="120">
          <template #default="{ row }">{{ row.prioritizeMatching ? '开启' : '关闭' }}</template>
        </el-table-column>
        <el-table-column prop="priorityCount" label="优先权" width="100" />
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确认移除该用户本周报名吗？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="removeRow(row)"
            >
              <template #reference>
                <el-button link type="danger" :disabled="row.status !== 'WAITING'" :loading="removingUserId === row.userId">
                  移除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="footer-tip">共 {{ total }} 条报名用户记录</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="prev, pager, next, total"
          :total="total"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMomentAdminEnrollments,
  getMomentAdminOverview,
  removeMomentAdminEnrollment,
} from '@/api/adminApi'
import type { MomentEnrollmentAdminItem } from '@/api/adminApi'

const loading = ref(false)
const removingUserId = ref<number | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<MomentEnrollmentAdminItem[]>([])
const filters = reactive({
  weekTag: '',
  pool: '',
  gender: undefined as number | undefined,
  status: '',
  keyword: '',
})

const serverWeekTag = ref('')
const overviewAuto = ref<{
  autoMatchEnabled: boolean
  autoMatchDayOfWeek: number
  autoMatchTime: string
  autoPublishEnabled: boolean
  autoPublishDayOfWeek: number
  autoPublishTime: string
} | null>(null)

const weekdayCn = ['一', '二', '三', '四', '五', '六', '日']

const autoMatchLine = computed(() => {
  const o = overviewAuto.value
  if (!o) return ''
  const parts: string[] = []
  if (o.autoMatchEnabled) {
    const d = o.autoMatchDayOfWeek >= 1 && o.autoMatchDayOfWeek <= 7 ? weekdayCn[o.autoMatchDayOfWeek - 1] : '?'
    parts.push(`当前已开启自动匹配：北京时间 周${d} ${o.autoMatchTime || '--'}`)
  } else {
    parts.push('当前未开启自动匹配（仅手动「触发匹配」时才会跑流水线）')
  }
  if (o.autoPublishEnabled) {
    const pd = o.autoPublishDayOfWeek >= 1 && o.autoPublishDayOfWeek <= 7 ? weekdayCn[o.autoPublishDayOfWeek - 1] : '?'
    parts.push(`自动公布：周${pd} ${o.autoPublishTime || '--'}（RESULT_READY 后生效）`)
  } else {
    parts.push('自动公布未开启时需手动「公布结果」后用户端才可见')
  }
  return parts.join('；')
})

function poolLabel(pool: string) {
  return { MF: 'MF 异性', FF: 'FF 女生', MM: 'MM 男生' }[pool] || pool
}

function genderText(gender?: number | null) {
  return gender === 1 ? '男' : gender === 2 ? '女' : '--'
}

function statusText(status: string) {
  return { WAITING: '待匹配', MATCHED: '已匹配', UNMATCHED: '未匹配' }[status] || status
}

function statusTagType(status: string) {
  return { WAITING: 'warning', MATCHED: 'success', UNMATCHED: 'info' }[status] || 'info'
}

function formatDateTime(value?: string | null) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMomentAdminEnrollments({
      page: page.value,
      size: size.value,
      weekTag: filters.weekTag || undefined,
      pool: filters.pool || undefined,
      gender: filters.gender,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
    })
    const data = res.data.data
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载报名名单失败')
  } finally {
    loading.value = false
  }
}

async function removeRow(row: MomentEnrollmentAdminItem) {
  removingUserId.value = row.userId
  try {
    await removeMomentAdminEnrollment(row.userId, row.weekTag)
    ElMessage.success('报名已移除')
    await loadData()
  } catch {
    ElMessage.error('移除报名失败')
  } finally {
    removingUserId.value = null
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.weekTag = ''
  filters.pool = ''
  filters.gender = undefined
  filters.status = ''
  filters.keyword = ''
  handleSearch()
}

async function loadOverviewHint() {
  try {
    const res = await getMomentAdminOverview()
    const o = res.data.data
    if (!o) return
    serverWeekTag.value = o.weekTag || ''
    overviewAuto.value = {
      autoMatchEnabled: !!o.autoMatchEnabled,
      autoMatchDayOfWeek: o.autoMatchDayOfWeek ?? 1,
      autoMatchTime: o.autoMatchTime || '',
      autoPublishEnabled: !!o.autoPublishEnabled,
      autoPublishDayOfWeek: o.autoPublishDayOfWeek ?? 5,
      autoPublishTime: o.autoPublishTime || '',
    }
  } catch {
    /* 提示失败不阻断名单 */
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadOverviewHint()])
})
</script>

<style lang="scss" scoped>
.page-tip {
  margin-bottom: 16px;
}

.tip-lines {
  margin: 0 0 6px;
  line-height: 1.55;
  font-size: 13px;
}

.tip-lines.muted {
  margin-bottom: 0;
  color: var(--el-text-color-secondary);
}

.moment-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card,
.table-card {
  border-radius: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-main {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.field-week {
  width: 180px;
}

.field-short {
  width: 140px;
}

.field-keyword {
  width: 260px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-meta {
  color: #6b7280;
  font-size: 13px;
}

.stack-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.footer-tip {
  color: #6b7280;
  font-size: 13px;
}

@media (max-width: 900px) {
  .field-week,
  .field-short,
  .field-keyword {
    width: 100%;
  }

  .table-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
