<template>
  <div class="admin-profile-page">
    <div class="page-header">
      <h1>人物画像管理</h1>
      <p class="page-desc">查看画像生成状态，支持手动触发生成</p>
    </div>

    <div v-if="loadError" class="error-card">
      <div class="error-icon">⚠️</div>
      <p class="error-msg">{{ loadError }}</p>
      <p class="error-hint">请执行数据库脚本创建 V24 相关表：<br />db/V24__rag_ai_profile.sql 或 db/fix_v24_tables.sql（仅建表）</p>
      <button class="btn-retry" @click="loadStats">重试加载</button>
    </div>

    <div class="stats-card">
      <h3>当前状态</h3>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="stats-grid">
        <div class="stat-item">
          <span class="stat-value">{{ stats.totalUsers }}</span>
          <span class="stat-label">总用户数</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats.profileCompleteCount }}</span>
          <span class="stat-label">已完善资料</span>
        </div>
        <div class="stat-item highlight">
          <span class="stat-value">{{ stats.hasProfileCount }}</span>
          <span class="stat-label">已有 AI 画像</span>
        </div>
        <div class="stat-item" :class="{ warn: stats.missingCount > 0 }">
          <span class="stat-value">{{ stats.missingCount }}</span>
          <span class="stat-label">待补充画像</span>
        </div>
      </div>
    </div>

    <div class="logic-card">
      <h3>画像生成逻辑</h3>
      <ul class="logic-list">
        <li><strong>初次生成</strong>：用户资料首次完善时自动触发（异步）</li>
        <li><strong>定时更新</strong>：短期每两周（周一 2:00）、长期每月 1 日 3:00（当前为占位实现）</li>
        <li><strong>手动触发</strong>：管理员可对指定用户或批量补充</li>
      </ul>
    </div>

    <div class="action-card">
      <h3>操作</h3>
      <div class="action-row">
        <div class="action-item">
          <label>单个用户</label>
          <div class="input-row">
            <el-input v-model.number="targetUserId" placeholder="用户 ID" type="number" style="width: 120px" />
            <el-checkbox v-model="forceOverwrite">覆盖已有画像</el-checkbox>
            <button
              class="btn-primary"
              :disabled="!targetUserId || regenerating"
              @click="doRegenerateOne"
            >
              {{ regenerating ? '生成中...' : '生成画像' }}
            </button>
          </div>
        </div>
        <div class="action-item">
          <label>批量补充</label>
          <p class="action-hint">为所有「已完善资料但无画像」的用户生成画像</p>
          <button
            class="btn-primary"
            :disabled="(loadSuccess && stats.missingCount === 0) || batchRegenerating"
            @click="doBatchRegenerate"
          >
            {{ batchRegenerating ? '处理中...' : (loadSuccess ? `批量补充（${stats.missingCount} 人）` : '尝试批量补充') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfileStats, regenerateProfile, batchRegenerateMissing, type ProfileStats } from '@/api/adminApi'

const stats = ref<ProfileStats>({
  totalUsers: 0,
  profileCompleteCount: 0,
  hasProfileCount: 0,
  missingCount: 0,
})
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadSuccess = ref(false)
const regenerating = ref(false)
const batchRegenerating = ref(false)
const targetUserId = ref<number | null>(null)
const forceOverwrite = ref(false)

async function loadStats() {
  loading.value = true
  loadError.value = null
  try {
    const res = await getProfileStats()
    stats.value = res.data.data
    loadSuccess.value = true
  } catch (err: any) {
    loadSuccess.value = false
    loadError.value = (err?.response?.data?.message ?? err?.message) || '加载失败'
    stats.value = { totalUsers: 0, profileCompleteCount: 0, hasProfileCount: 0, missingCount: 0 }
  } finally {
    loading.value = false
  }
}

async function doRegenerateOne() {
  const uid = targetUserId.value
  if (!uid) return
  regenerating.value = true
  try {
    await regenerateProfile(uid, forceOverwrite.value)
    ElMessage.success('已提交生成任务，后台异步执行')
    await loadStats()
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    regenerating.value = false
  }
}

async function doBatchRegenerate() {
  batchRegenerating.value = true
  try {
    const res = await batchRegenerateMissing()
    const count = res.data.data ?? 0
    ElMessage.success(`已提交 ${count} 个用户的画像生成任务`)
    await loadStats()
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    batchRegenerating.value = false
  }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.admin-profile-page {
  padding: 24px;
  max-width: 720px;
}

.page-header {
  margin-bottom: 24px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .page-desc {
    font-size: 14px;
    color: $text-secondary;
  }
}

.stats-card,
.logic-card,
.action-card {
  background: $bg-primary;
  border-radius: $radius-lg;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: $shadow-sm;

  h3 {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: $text-primary;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: $bg-secondary;
  border-radius: $radius-md;

  &.highlight .stat-value {
    color: $primary;
  }

  &.warn .stat-value {
    color: #e6a23c;
  }

  .stat-value {
    display: block;
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
  }

  .stat-label {
    font-size: 12px;
    color: $text-secondary;
  }
}

.loading {
  padding: 20px;
  text-align: center;
  color: $text-muted;
}

.error-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  margin-bottom: 20px;
  background: rgba($warning, 0.08);
  border: 1px solid rgba($warning, 0.3);
  border-radius: $radius-lg;

  .error-icon { font-size: 32px; margin-bottom: 16px; }
  .error-msg { font-size: 15px; color: $text-primary; margin-bottom: 8px; font-weight: 500; }
  .error-hint { font-size: 13px; color: $text-muted; margin-bottom: 16px; text-align: center; }
  .btn-retry {
    padding: 8px 24px;
    background: $primary;
    color: white;
    border: none;
    border-radius: $radius-md;
    font-size: 14px;
    cursor: pointer;
    &:hover { opacity: 0.9; }
  }
}

.logic-list {
  margin: 0;
  padding-left: 20px;
  color: $text-secondary;
  line-height: 1.8;

  li {
    margin-bottom: 4px;
  }
}

.action-row {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.action-item {
  label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 8px;
    color: $text-primary;
  }

  .input-row {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .action-hint {
    font-size: 13px;
    color: $text-muted;
    margin-bottom: 8px;
  }

  .btn-primary {
    padding: 8px 20px;
    border-radius: $radius-md;
    font-size: 14px;
    cursor: pointer;
    border: none;
    background: $primary;
    color: white;
    transition: opacity 0.2s;

    &:hover:not(:disabled) {
      opacity: 0.9;
    }

    &:disabled {
      background: $border-color;
      cursor: not-allowed;
    }
  }
}
</style>
