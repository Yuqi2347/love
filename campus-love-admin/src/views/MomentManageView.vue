<template>
  <div class="moment-manage">
    <!-- 状态概览 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">当前周期</div>
          <div class="stat-value">{{ statusInfo.currentWeek || '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">报名状态</div>
          <div class="stat-value" :class="statusInfo.enrollmentOpen ? 'text-success' : 'text-danger'">
            {{ statusInfo.enrollmentOpen ? '开放中' : '已截止' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">参与人数</div>
          <div class="stat-value">{{ statusInfo.participantCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">活动状态</div>
          <div class="stat-value">{{ statusText }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作面板 -->
    <el-card class="action-card">
      <template #header>
        <div class="card-header">
          <span>活动管理</span>
          <el-button type="info" :icon="Refresh" circle @click="loadStatus" :loading="loading" />
        </div>
      </template>

      <el-alert
        v-if="lastResult"
        :title="lastResult"
        :type="lastResultType"
        show-icon
        closable
        class="result-alert"
        @close="lastResult = ''"
      />

      <div class="action-group">
        <div class="action-item">
          <div class="action-info">
            <h4>截止报名</h4>
            <p>手动截止本周报名，截止后用户不可再报名参加</p>
          </div>
          <el-button
            type="warning"
            :disabled="!statusInfo.enrollmentOpen"
            :loading="actionLoading === 'close'"
            @click="handleClose"
          >
            截止报名
          </el-button>
        </div>

        <el-divider />

        <div class="action-item">
          <div class="action-info">
            <h4>触发匹配</h4>
            <p>执行本周匹配算法（会自动截止报名），匹配完成后用户可查看结果</p>
          </div>
          <el-button
            type="primary"
            :loading="actionLoading === 'trigger'"
            @click="handleTrigger"
          >
            触发匹配
          </el-button>
        </div>

        <el-divider />

        <div class="action-item">
          <div class="action-info">
            <h4>重新开放报名</h4>
            <p>重新开放本周报名通道（调试用）</p>
          </div>
          <el-button
            type="success"
            :disabled="statusInfo.enrollmentOpen"
            :loading="actionLoading === 'reopen'"
            @click="handleReopen"
          >
            重新开放
          </el-button>
        </div>

        <el-divider />

        <div class="action-item">
          <div class="action-info">
            <h4>重置本周活动</h4>
            <p>删除匹配结果 + 重置报名状态 + 重新开放报名（调试用，操作不可逆）</p>
          </div>
          <el-popconfirm
            title="确定要重置本周活动吗？此操作将删除所有匹配结果！"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleReset"
          >
            <template #reference>
              <el-button
                type="danger"
                :loading="actionLoading === 'reset'"
              >
                重置本周
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getMomentStatus,
  triggerMomentMatching,
  closeMomentEnrollment,
  reopenMomentEnrollment,
  resetMomentWeek,
} from '@/api/adminApi'
import type { MomentStatusInfo } from '@/api/adminApi'

const loading = ref(false)
const actionLoading = ref('')
const lastResult = ref('')
const lastResultType = ref<'success' | 'error' | 'warning'>('success')

const statusInfo = ref<MomentStatusInfo>({
  currentWeek: '',
  status: '',
  participantCount: 0,
  enrollmentOpen: false,
})

const statusText = computed(() => {
  const s = statusInfo.value.status
  if (!s) return '--'
  const map: Record<string, string> = {
    NOT_ENROLLED: '未开始',
    WAITING: '等待匹配',
    MATCHED: '已匹配',
    UNMATCHED: '未匹配',
  }
  return map[s] || s
})

async function loadStatus() {
  loading.value = true
  try {
    const res = await getMomentStatus()
    if (res.data.data) {
      statusInfo.value = res.data.data
    }
  } catch {
    lastResult.value = '获取状态失败'
    lastResultType.value = 'error'
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  actionLoading.value = 'close'
  try {
    await closeMomentEnrollment()
    lastResult.value = '已截止报名'
    lastResultType.value = 'success'
    await loadStatus()
  } catch {
    lastResult.value = '截止报名失败'
    lastResultType.value = 'error'
  } finally {
    actionLoading.value = ''
  }
}

async function handleTrigger() {
  actionLoading.value = 'trigger'
  try {
    const res = await triggerMomentMatching()
    const data = res.data.data
    const matchedPairs = typeof data?.matchedPairs === 'number' ? data.matchedPairs : 0
    const unmatchedUsers = typeof data?.unmatchedUsers === 'number' ? data.unmatchedUsers : 0
    const baseThreshold = data?.baseThreshold
    lastResult.value = baseThreshold != null
      ? `匹配完成！配对 ${matchedPairs} 对，未匹配 ${unmatchedUsers} 人，当前基础阈值 ${baseThreshold}`
      : `匹配完成！配对 ${matchedPairs} 对，未匹配 ${unmatchedUsers} 人`
    lastResultType.value = 'success'
    await loadStatus()
  } catch {
    lastResult.value = '触发匹配失败'
    lastResultType.value = 'error'
  } finally {
    actionLoading.value = ''
  }
}

async function handleReopen() {
  actionLoading.value = 'reopen'
  try {
    await reopenMomentEnrollment()
    lastResult.value = '已重新开放报名'
    lastResultType.value = 'success'
    await loadStatus()
  } catch {
    lastResult.value = '重新开放失败'
    lastResultType.value = 'error'
  } finally {
    actionLoading.value = ''
  }
}

async function handleReset() {
  actionLoading.value = 'reset'
  try {
    await resetMomentWeek()
    lastResult.value = '已重置本周活动'
    lastResultType.value = 'warning'
    await loadStatus()
  } catch {
    lastResult.value = '重置失败'
    lastResultType.value = 'error'
  } finally {
    actionLoading.value = ''
  }
}

onMounted(loadStatus)
</script>

<style lang="scss" scoped>
.moment-manage {
  max-width: 1000px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
  }
  .text-success { color: #67c23a; }
  .text-danger { color: #f56c6c; }
}

.action-card {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    font-size: 16px;
  }
}

.result-alert {
  margin-bottom: 20px;
}

.action-group {
  .action-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
  }
  .action-info {
    h4 {
      margin: 0 0 4px;
      font-size: 15px;
      color: #303133;
    }
    p {
      margin: 0;
      font-size: 13px;
      color: #909399;
    }
  }
}
</style>
