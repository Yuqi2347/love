<template>
  <div class="invite-history-page">
    <div class="page-header">
      <button class="btn-text" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="page-title">邀约历史</h1>
      <div></div>
    </div>

    <!-- Stats Card -->
    <div v-if="stats" class="stats-card">
      <h3 class="stats-title">我的邀约统计</h3>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ stats.inviteCount }}</div>
          <div class="stat-label">发起邀约</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.participateCount }}</div>
          <div class="stat-label">参与邀约</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.successRate.toFixed(1) }}%</div>
          <div class="stat-label">成功率</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.receivedSocialRating?.toFixed(1) || '-' }}</div>
          <div class="stat-label">社交评分</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.receivedOrgRating?.toFixed(1) || '-' }}</div>
          <div class="stat-label">组织评分</div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs-row">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Invite List -->
    <div class="invite-list">
      <div v-if="loading" class="loading-hint">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="currentInvites.length" class="invite-items">
        <div
          v-for="invite in currentInvites"
          :key="invite.id"
          class="invite-card"
          @click="$router.push(`/invite/${invite.id}`)"
        >
          <div class="invite-header">
            <div class="invite-type-badge" :style="{ background: getTypeColor(invite.inviteType) }">
              {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
            </div>
            <div class="invite-status-badge" :style="{ color: INVITE_STATUS_COLORS[invite.status as InviteStatus] }">
              {{ INVITE_STATUS_LABELS[invite.status as InviteStatus] }}
            </div>
          </div>

          <h3 class="invite-title">{{ invite.title }}</h3>
          <p v-if="invite.content" class="invite-content">{{ invite.content }}</p>

          <div class="invite-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatDateTime(invite.inviteTime) }}
            </span>
            <span v-if="invite.location" class="meta-item">
              <el-icon><Location /></el-icon>
              {{ invite.location }}
            </span>
          </div>

          <div class="invite-footer">
            <div class="participants-info">
              <el-icon><UserFilled /></el-icon>
              {{ invite.participantCount }}/{{ invite.maxParticipants || '不限' }}人
            </div>
            <div v-if="invite.ratingCount" class="rating-info">
              ⭐ {{ invite.socialRating?.toFixed(1) || '-' }} ({{ invite.ratingCount }})
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">
        <div class="empty-icon">{{ activeTab === 'created' ? '📝' : '👥' }}</div>
        <p>{{ activeTab === 'created' ? '还没有发起过邀约' : '还没有参与过邀约' }}</p>
        <button class="btn-outline" @click="$router.push('/invite/create')">
          {{ activeTab === 'created' ? '发起邀约' : '浏览邀约' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import { getInviteStats, type InviteStats } from '@/api/inviteApi'
import { ArrowLeft, Clock, Location, UserFilled } from '@element-plus/icons-vue'
import {
  InviteType,
  InviteStatus,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
} from '@/constants/inviteConst'
import { getTypeColor } from '@/utils/shared'

const userStore = useUserStore()
const inviteStore = useInviteStore()

const activeTab = ref<string>('created')
const loading = ref(false)
const stats = ref<InviteStats | null>(null)

const tabs = [
  { label: '我发起的', value: 'created' },
  { label: '我参与的', value: 'participated' },
]

// 当前邀约列表（这里简化处理，实际需要单独的API）
const currentInvites = computed(() => {
  if (!userStore.user) return []
  const userId = userStore.user.id

  if (activeTab.value === 'created') {
    return inviteStore.invites.filter(i => i.creatorId === userId)
  } else {
    return inviteStore.invites.filter(i =>
      i.participants?.some(p => p.userId === userId)
    )
  }
})

// 格式化日期时间
function formatDateTime(timeStr: string): string {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diff < 86400) return '今天'
  if (diff < 172800) return '昨天'
  return date.toLocaleDateString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
  })
}

// 加载统计信息
async function loadStats() {
  try {
    const res = await getInviteStats()
    stats.value = res.data.data
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

onMounted(() => {
  inviteStore.fetchInvites()
  loadStats()
})
</script>

<style lang="scss" scoped>
.invite-history-page { padding: 20px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stats-card {
  background: linear-gradient(135deg, rgba($primary, 0.1), rgba($primary, 0.05));
  border: 1px solid rgba($primary, 0.2);
  border-radius: $radius-lg;
  padding: 20px;
  margin-bottom: 20px;
}

.stats-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: $text-primary;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: $primary;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
}

.tabs-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
  }
}

.invite-list {
  min-height: 400px;
}

.loading-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: $text-muted;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid $border-light;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.invite-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invite-card {
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 16px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary;
    box-shadow: $shadow-sm;
  }
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.invite-type-badge {
  padding: 4px 12px;
  color: white;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
}

.invite-status-badge {
  font-size: 13px;
  font-weight: 600;
}

.invite-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 6px;
}

.invite-content {
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.5;
  margin-bottom: 10px;
}

.invite-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: $text-muted;
}

.invite-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid $border-light;
}

.participants-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: $text-muted;
}

.rating-info {
  margin-left: auto;
  font-size: 12px;
  color: $text-secondary;
}

.empty-hint {
  text-align: center;
  padding: 60px 20px;
  color: $text-muted;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-hint p {
  font-size: 14px;
  margin-bottom: 16px;
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: color $transition-fast;

  &:hover { color: $primary; }
}
</style>
