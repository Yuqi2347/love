<template>
  <div class="user-list">
    <h2 class="page-title">用户管理</h2>
    <el-card>
      <el-form inline class="toolbar">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="昵称/邮箱" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="school" label="学校" width="120" />
        <el-table-column label="信用分" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.creditScore"
              :min="0"
              :max="1000"
              size="small"
              controls-position="right"
              style="width: 90px"
            />
          </template>
        </el-table-column>
        <el-table-column label="活跃度" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.activityScore"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 80px"
            />
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.userLevel"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 80px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="inviteCount" label="发起" width="70" />
        <el-table-column prop="participateCount" label="参与" width="70" />
        <el-table-column label="管理员" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isAdmin" type="danger" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="saveStats(row)">保存</el-button>
            <el-button
              v-if="!row.isAdmin"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsers, updateUserStats, deleteAdminUser, type AdminUserItem } from '@/api/adminApi'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<AdminUserItem[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getAdminUsers({ page: page.value, size: size.value, keyword: keyword.value || undefined })
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

async function saveStats(row: AdminUserItem) {
  try {
    const data: { creditScore?: number; activityScore?: number; userLevel?: number } = {}
    if (row.creditScore != null) data.creditScore = row.creditScore
    if (row.activityScore != null) data.activityScore = row.activityScore
    if (row.userLevel != null) data.userLevel = row.userLevel
    await updateUserStats(row.id, data)
    ElMessage.success('已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(row: AdminUserItem) {
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除用户「${row.nickname}」（${row.email}）及其全部相关数据吗？此操作不可恢复。`,
      '删除用户',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await deleteAdminUser(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 用户取消不提示；其他错误由 request 拦截器已展示
    if (e === 'cancel') return
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.page-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; }

.toolbar { margin-bottom: 16px; }

.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
