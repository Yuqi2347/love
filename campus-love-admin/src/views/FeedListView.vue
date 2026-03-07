<template>
  <div class="feed-list">
    <h2 class="page-title">帖子管理</h2>
    <el-card>
      <el-form inline class="toolbar">
        <el-form-item label="用户ID">
          <el-input v-model="userId" placeholder="筛选作者" clearable style="width: 120px" type="number" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="nickname" label="作者" width="100" />
        <el-table-column prop="content" label="内容摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="postType" label="类型" width="90">
          <template #default="{ row }">{{ row.postType === 'DISCOVERY' ? '发现' : '朋友圈' }}</template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="70" />
        <el-table-column prop="commentCount" label="评论" width="70" />
        <el-table-column prop="createdAt" label="发布时间" width="170">
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
import { getAdminFeeds, deleteAdminFeed, type AdminFeedItem } from '@/api/adminApi'

const loading = ref(false)
const userId = ref<number | ''>('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<AdminFeedItem[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getAdminFeeds({
      page: page.value,
      size: size.value,
      userId: userId.value === '' ? undefined : Number(userId.value),
    })
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

async function handleDelete(row: AdminFeedItem) {
  try {
    await ElMessageBox.confirm('确定删除该帖子吗？', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAdminFeed(row.id)
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
