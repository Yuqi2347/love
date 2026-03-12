<template>
  <div class="report-list">
    <h2 class="page-title">举报管理</h2>
    <el-card>
      <el-form inline class="toolbar">
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已审核" value="REVIEWED" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input v-model="targetIdFilter" placeholder="帖子/评论ID" clearable style="width: 120px" type="number" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="reporterNickname" label="举报人" width="100" />
        <el-table-column prop="targetType" label="类型" width="90">
          <template #default="{ row }">
            {{ row.targetType === 'POST' ? '帖子' : row.targetType === 'COMMENT' ? '评论' : row.targetType }}
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" width="90" />
        <el-table-column prop="targetSummary" label="内容摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="violationTypes" label="违规类型" width="120" show-overflow-tooltip />
        <el-table-column prop="reason" label="举报理由" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PENDING'" type="warning" size="small">待处理</el-tag>
            <el-tag v-else-if="row.status === 'REVIEWED'" type="info" size="small">已审核</el-tag>
            <el-tag v-else type="success" size="small">已解决</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminNote" label="管理员备注" width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="举报时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              link
              size="small"
              @click="openReview(row)"
            >
              审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-dialog v-model="reviewVisible" title="审核举报" width="480px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="管理员备注">
          <el-input v-model="reviewForm.adminNote" type="textarea" :rows="3" placeholder="填写审核结果或反馈" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="reviewForm.status" style="width: 100%">
            <el-option label="已审核" value="REVIEWED" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReportList, reviewReport, type ReportItem } from '@/api/adminApi'

const loading = ref(false)
const statusFilter = ref('')
const targetIdFilter = ref<number | ''>('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<ReportItem[]>([])

const reviewVisible = ref(false)
const reviewing = ref(false)
const currentReport = ref<ReportItem | null>(null)
const reviewForm = ref({ adminNote: '', status: 'REVIEWED' })

async function load() {
  loading.value = true
  try {
    const res = await getReportList({
      page: page.value - 1,
      size: size.value,
      status: statusFilter.value || undefined,
      targetId: targetIdFilter.value === '' ? undefined : Number(targetIdFilter.value),
    })
    const data = res.data.data
    tableData.value = Array.isArray(data) ? data : []
    total.value = Math.max((page.value - 1) * size.value + tableData.value.length, tableData.value.length)
  } finally {
    loading.value = false
  }
}

function formatTime(s: string) {
  if (!s) return '-'
  return new Date(s).toLocaleString('zh-CN')
}

function openReview(row: ReportItem) {
  currentReport.value = row
  reviewForm.value = { adminNote: row.adminNote || '', status: 'REVIEWED' }
  reviewVisible.value = true
}

async function submitReview() {
  if (!currentReport.value) return
  reviewing.value = true
  try {
    await reviewReport(currentReport.value.id, reviewForm.value)
    ElMessage.success('审核完成')
    reviewVisible.value = false
    load()
  } finally {
    reviewing.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; }

.toolbar { margin-bottom: 16px; }

.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
