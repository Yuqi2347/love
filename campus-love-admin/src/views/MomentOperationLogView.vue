<template>
  <div class="moment-page">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-main">
          <el-input v-model="filters.weekTag" placeholder="活动周，例如 2026-W12" clearable class="field-week" />
          <el-select v-model="filters.actionType" placeholder="操作类型" clearable class="field-action">
            <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
          <span>操作结果日志</span>
          <span class="panel-meta">记录人工操作与系统自动执行结果</span>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="weekTag" label="周次" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="140" />
        <el-table-column label="动作" width="170">
          <template #default="{ row }">
            <el-tag effect="light" :type="actionTagType(row.actionType)">{{ actionText(row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="100">
          <template #default="{ row }">{{ row.targetId ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="summary" label="执行结果" min-width="320" />
        <el-table-column label="详情" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="footer-tip">共 {{ total }} 条日志</div>
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

    <el-drawer v-model="detailVisible" size="600px" destroy-on-close>
      <template #header>
        <div class="drawer-title">日志详情</div>
      </template>

      <div v-if="detailRow" class="detail-body">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="时间">{{ formatDateTime(detailRow.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="周次">{{ detailRow.weekTag || '--' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detailRow.operatorName }}</el-descriptions-item>
          <el-descriptions-item label="动作">{{ actionText(detailRow.actionType) }}</el-descriptions-item>
          <el-descriptions-item label="目标">{{ detailRow.targetType }} / {{ detailRow.targetId ?? '--' }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detailRow.summary || '--' }}</el-descriptions-item>
        </el-descriptions>

        <el-card class="json-card">
          <template #header>detail_json</template>
          <pre>{{ prettyJson(detailRow.detailJson) }}</pre>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMomentAdminLogs } from '@/api/adminApi'
import type { MomentOperationLogItem } from '@/api/adminApi'

const loading = ref(false)
const detailVisible = ref(false)
const detailRow = ref<MomentOperationLogItem | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<MomentOperationLogItem[]>([])
const filters = reactive({
  weekTag: '',
  actionType: '',
})

const actionOptions = [
  { label: '人工截止报名', value: 'MANUAL_CLOSE' },
  { label: '系统截止报名', value: 'AUTO_CLOSE' },
  { label: '人工触发匹配', value: 'MANUAL_TRIGGER_MATCH' },
  { label: '系统自动匹配', value: 'AUTO_TRIGGER_MATCH' },
  { label: '重新开放报名', value: 'MANUAL_REOPEN' },
  { label: '重置本周活动', value: 'RESET_WEEK' },
  { label: '移除报名用户', value: 'REMOVE_ENROLLMENT' },
]

function actionText(action: string) {
  return {
    MANUAL_CLOSE: '人工截止报名',
    AUTO_CLOSE: '系统截止报名',
    MANUAL_TRIGGER_MATCH: '人工触发匹配',
    AUTO_TRIGGER_MATCH: '系统自动匹配',
    MANUAL_REOPEN: '重新开放报名',
    RESET_WEEK: '重置本周活动',
    REMOVE_ENROLLMENT: '移除报名用户',
  }[action] || action
}

function actionTagType(action: string) {
  if (action.includes('TRIGGER')) return 'success'
  if (action.includes('CLOSE')) return 'warning'
  if (action === 'RESET_WEEK') return 'danger'
  return 'info'
}

function formatDateTime(value?: string | null) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function prettyJson(value?: string | null) {
  if (!value) return '--'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMomentAdminLogs({
      page: page.value,
      size: size.value,
      weekTag: filters.weekTag || undefined,
      actionType: filters.actionType || undefined,
    })
    const data = res.data.data
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载操作日志失败')
  } finally {
    loading.value = false
  }
}

function openDetail(row: MomentOperationLogItem) {
  detailRow.value = row
  detailVisible.value = true
}

function handleSearch() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.weekTag = ''
  filters.actionType = ''
  handleSearch()
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.moment-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card,
.table-card,
.json-card {
  border-radius: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-main,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.field-week {
  width: 180px;
}

.field-action {
  width: 220px;
}

.panel-header,
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-meta,
.footer-tip {
  color: #6b7280;
  font-size: 13px;
}

.table-footer {
  margin-top: 16px;
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

pre {
  margin: 0;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 900px) {
  .field-week,
  .field-action {
    width: 100%;
  }

  .table-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
