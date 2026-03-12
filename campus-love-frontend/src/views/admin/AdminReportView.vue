<template>
  <div class="admin-report-page">
    <div class="page-header">
      <h1>举报管理</h1>
      <div class="filters">
        <el-input
          v-model="filterTargetId"
          placeholder="帖子/评论 ID"
          clearable
          style="width: 140px"
          @keyup.enter="loadReports"
        />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已审核" value="REVIEWED" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <button class="btn-primary" @click="loadReports">查询</button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!reports.length" class="empty">暂无举报记录</div>
    <div v-else class="report-list">
      <div v-for="r in reports" :key="r.id" class="report-card">
        <div class="report-row">
          <span class="report-id">#{{ r.id }}</span>
          <span :class="['report-status', r.status?.toLowerCase()]">{{ statusLabel(r.status) }}</span>
          <span class="report-time">{{ formatTime(r.createdAt) }}</span>
        </div>
        <div class="report-meta">
          <span>举报人：{{ r.reporterNickname || '-' }}</span>
          <span>目标：{{ r.targetType }}#{{ r.targetId }}</span>
          <router-link v-if="r.targetType === 'POST'" :to="`/feed/${r.targetId}`" class="link-post">
            查看帖子
          </router-link>
        </div>
        <div class="report-content">
          <div>违规类型：{{ formatViolationTypes(r.violationTypes) }}</div>
          <div v-if="r.reason">理由：{{ r.reason }}</div>
          <div v-if="r.targetSummary" class="target-summary">内容摘要：{{ r.targetSummary }}</div>
        </div>
        <div v-if="r.adminNote" class="report-admin-note">
          <strong>举报反馈：</strong>{{ r.adminNote }}
        </div>
        <div class="report-actions">
          <el-input
            v-model="editAdminNote[r.id]"
            type="textarea"
            :rows="2"
            placeholder="填写举报反馈（可选）"
            maxlength="500"
            show-word-limit
            class="admin-note-input"
          />
          <div class="action-btns">
            <button
              class="btn-sm btn-primary"
              :disabled="reviewingId === r.id"
              @click="doReview(r.id, 'REVIEWED')"
            >
              {{ reviewingId === r.id ? '处理中...' : '标记已审核' }}
            </button>
            <button
              class="btn-sm btn-success"
              :disabled="reviewingId === r.id"
              @click="doReview(r.id, 'RESOLVED')"
            >
              标记已解决
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listReports, reviewReport, type ReportItem } from '@/api/reportApi'
import { useUserStore } from '@/store/userStore'
import { VIOLATION_TYPES } from '@/api/reportApi'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const reports = ref<ReportItem[]>([])
const loading = ref(false)
const filterTargetId = ref('')
const filterStatus = ref('')
const reviewingId = ref<number | null>(null)
const editAdminNote = ref<Record<number, string>>({})

function statusLabel(s: string | undefined) {
  const map: Record<string, string> = { PENDING: '待处理', REVIEWED: '已审核', RESOLVED: '已解决' }
  return map[s || ''] || s || '-'
}

function formatViolationTypes(v: string | undefined) {
  if (!v) return '-'
  const arr = v.split(',')
  const labels = arr.map((t) => VIOLATION_TYPES.find((x) => x.value === t)?.label || t)
  return labels.join('、')
}

function formatTime(s: string | undefined) {
  if (!s) return '-'
  try {
    const d = new Date(s)
    return d.toLocaleString('zh-CN')
  } catch {
    return s
  }
}

async function loadReports() {
  loading.value = true
  try {
    const targetId = filterTargetId.value.trim() ? parseInt(filterTargetId.value, 10) : undefined
    if (filterTargetId.value.trim() && isNaN(targetId!)) {
      reports.value = []
      return
    }
    const res = await listReports({
      page: 0,
      size: 50,
      status: filterStatus.value || undefined,
      targetId,
    })
    reports.value = res.data.data || []
    const notes: Record<number, string> = {}
    for (const r of reports.value) {
      if (r.adminNote) notes[r.id] = r.adminNote
    }
    editAdminNote.value = notes
  } catch {
    reports.value = []
  } finally {
    loading.value = false
  }
}

async function doReview(id: number, status: string) {
  reviewingId.value = id
  try {
    const note = editAdminNote.value[id]?.trim()
    await reviewReport(id, { adminNote: note || undefined, status })
    await loadReports()
  } catch {
    // error shown by interceptor
  } finally {
    reviewingId.value = null
  }
}

onMounted(() => {
  const tid = route.query.targetId
  if (tid && typeof tid === 'string') {
    filterTargetId.value = tid
  }
  if (!userStore.user?.isAdmin) {
    router.replace('/discover')
    return
  }
  loadReports()
})

watch(
  () => route.query.targetId,
  (tid) => {
    if (tid && typeof tid === 'string') filterTargetId.value = tid
  },
)
</script>

<style scoped lang="scss">
.admin-report-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;

  h1 {
    margin: 0;
    font-size: 1.25rem;
  }
}

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary, #666);
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-card {
  background: var(--card-bg, #fff);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.report-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.report-id {
  font-weight: 600;
  color: var(--text-primary, #333);
}

.report-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;

  &.pending {
    background: #fff3e0;
    color: #e65100;
  }

  &.reviewed {
    background: #e3f2fd;
    color: #1565c0;
  }

  &.resolved {
    background: #e8f5e9;
    color: #2e7d32;
  }
}

.report-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.report-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-secondary, #666);

  .link-post {
    color: var(--primary, #6366f1);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.report-content {
  font-size: 13px;
  margin-bottom: 8px;

  .target-summary {
    color: var(--text-secondary, #666);
    margin-top: 4px;
  }
}

.report-admin-note {
  background: #f5f5f5;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 12px;
}

.report-actions {
  border-top: 1px solid #eee;
  padding-top: 12px;

  .admin-note-input {
    margin-bottom: 8px;
  }

  .action-btns {
    display: flex;
    gap: 8px;
  }
}

.btn-sm {
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  border: none;
  cursor: pointer;

  &.btn-primary {
    background: var(--primary, #6366f1);
    color: #fff;
  }

  &.btn-success {
    background: #2e7d32;
    color: #fff;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>
