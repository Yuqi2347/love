<template>
  <div class="invite-list">
    <h2 class="page-title">邀约管理</h2>
    <el-card>
      <el-form inline class="toolbar">
        <el-form-item label="状态">
          <el-select v-model="status" placeholder="全部" clearable style="width: 120px">
            <el-option label="招募中" value="RECRUITING" />
            <el-option label="已满员" value="FULL" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已结束" value="ENDED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="creatorNickname" label="发起人" width="100" />
        <el-table-column prop="inviteType" label="类型" width="90" />
        <el-table-column prop="inviteMode" label="模式" width="80">
          <template #default="{ row }">{{ row.inviteMode === 'PUBLIC' ? '公开' : '一对一' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人数" width="80">
          <template #default="{ row }">{{ row.participantCount }}/{{ row.maxParticipants ?? '不限' }}</template>
        </el-table-column>
        <el-table-column prop="inviteTime" label="活动时间" width="170">
          <template #default="{ row }">{{ formatTime(row.inviteTime) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminInvites, deleteAdminInvite, type AdminInviteItem } from '@/api/adminApi'

const loading = ref(false)
const status = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<AdminInviteItem[]>([])

const statusLabels: Record<string, string> = {
  RECRUITING: '招募中',
  FULL: '已满员',
  CONFIRMED: '已确认',
  IN_PROGRESS: '进行中',
  ENDED: '已结束',
  CANCELLED: '已取消',
}

function statusLabel(s: string) {
  return statusLabels[s] || s
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminInvites({ page: page.value, size: size.value, status: status.value || undefined })
    const data = res.data.data
    tableData.value = data?.records ?? []
    total.value = data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function formatTime(s: string) {
  if (!s) return '-'
  return new Date(s).toLocaleString('zh-CN')
}

async function handleDelete(row: AdminInviteItem) {
  try {
    await ElMessageBox.confirm('确定删除该邀约吗？删除后不可恢复。', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAdminInvite(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; }

.toolbar { margin-bottom: 16px; }

.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
