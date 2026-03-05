<template>
  <div class="invite-page">
    <div class="page-header">
      <h1 class="page-title">我的邀约</h1>
      <button class="btn-primary" @click="$router.push('/invite/create')">
        <el-icon><Plus /></el-icon> 发起邀约
      </button>
    </div>

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

    <div v-if="activeTab === 'list' && recommendList.length" class="recommend-section">
      <div class="recommend-title-row">
        <h2 class="recommend-title">你可能会感兴趣的邀约</h2>
      </div>
      <div class="recommend-list">
        <div
          v-for="item in recommendList"
          :key="item.id"
          class="recommend-card"
          @click="$router.push(`/invite/${item.id}`)"
        >
          <div class="recommend-header">
            <span class="invite-type-badge" :style="{ background: getTypeColor(item.inviteType) }">
              {{ INVITE_TYPE_LABELS[item.inviteType as InviteType] }}
            </span>
            <span v-if="item.isUrgent" class="urgent-tag">急需</span>
          </div>
          <div class="recommend-title-text">
            {{ item.title }}
          </div>
          <div class="recommend-meta">
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatInviteTime(item.inviteTime) }}
            </span>
            <span v-if="item.location">
              <el-icon><Location /></el-icon>
              {{ item.location }}
            </span>
            <span>
              <el-icon><UserFilled /></el-icon>
              {{ item.participantCount }}/{{ item.maxParticipants || '不限' }}人
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="filters-row" v-if="activeTab === 'list'">
      <el-select
        v-model="filterType"
        placeholder="邀约类型"
        clearable
        @change="handleFilterChange"
      >
        <el-option
          v-for="opt in INVITE_TYPE_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>

      <el-select
        v-model="filterStatus"
        placeholder="邀约状态"
        clearable
        @change="handleFilterChange"
      >
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </div>

    <div v-if="activeTab === 'list'" class="invite-list">
      <div v-if="inviteStore.loading && !inviteStore.invites.length" class="loading-hint">
        加载中...
      </div>
      <div v-else-if="inviteStore.invites.length" class="invite-items">
        <div
          v-for="invite in filteredInvites"
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
              <el-icon><User /></el-icon>
              {{ invite.creator?.nickname || '未知' }}
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatInviteTime(invite.inviteTime) }}
            </span>
            <span v-if="invite.location" class="meta-item">
              <el-icon><Location /></el-icon>
              {{ invite.location }}
            </span>
          </div>

          <div class="invite-footer">
            <div class="participants-info">
              <el-icon><UserFilled /></el-icon>
              {{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}人
            </div>
            <div v-if="invite.isUrgent" class="urgent-tag">急需</div>
            <div v-if="invite.ratingCount" class="rating-info">
              ⭐ {{ invite.socialRating?.toFixed(1) || '-' }} ({{ invite.ratingCount }})
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">
        <div class="empty-icon">📅</div>
        <p>暂无邀约，快发起一个吧！</p>
      </div>
    </div>

    <div v-else-if="activeTab === 'wait'" class="wait-list">
      <div class="wait-header-row">
        <h2 class="section-title">我的等待邀约</h2>
        <button class="btn-outline" @click="$router.push('/invite/wait')">
          创建等待邀约
        </button>
      </div>
      <div v-if="waitList.length" class="wait-items">
        <div
          v-for="wait in waitList"
          :key="wait.id"
          class="wait-card"
        >
          <div class="wait-header">
            <div class="wait-types">
              <span
                v-for="type in parseWaitTypes(wait.inviteTypes)"
                :key="type"
                class="wait-type-tag"
              >
                {{ type }}
              </span>
            </div>
            <div class="wait-status-tags">
              <span v-if="wait.isExpired" class="expired-tag">已过期</span>
              <span v-else class="pending-tag">等待匹配</span>
              <span v-if="wait.autoAccept" class="auto-accept-tag">自动受邀</span>
            </div>
          </div>
          <div v-if="wait.periodConfig" class="wait-info">
            <span>时间偏好: {{ formatWaitPeriod(wait.periodConfig) }}</span>
          </div>
          <div v-if="wait.locationPref" class="wait-info">
            <span>地点: {{ wait.locationPref }}</span>
          </div>
          <div class="wait-footer">
            <span class="expire-time">有效期至: {{ formatExpireTime(wait.expireTime) }}</span>
            <button class="btn-text danger" @click="handleCancelWait(wait.id)">取消</button>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">
        <div class="empty-icon">⏳</div>
        <p>暂无等待邀约</p>
        <button class="btn-outline" @click="$router.push('/invite/wait')">创建等待邀约</button>
      </div>
    </div>

    <div v-else-if="activeTab === 'history'" class="history-list">
      <div v-if="stats" class="stats-card">
        <h3 class="stats-title">邀约统计</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.inviteCount }}</span>
            <span class="stat-label">发起邀约</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.participateCount }}</span>
            <span class="stat-label">参与邀约</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.successRate.toFixed(1) }}%</span>
            <span class="stat-label">成功率</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.receivedSocialRating?.toFixed(1) || '-' }}</span>
            <span class="stat-label">社交评分</span>
          </div>
        </div>
      </div>

      <div class="history-filters">
        <div class="history-range">
          <button
            v-for="r in historyRanges"
            :key="r.value"
            :class="['range-btn', { active: historyRange === r.value }]"
            @click="changeRange(r.value)"
          >
            {{ r.label }}
          </button>
        </div>
        <div class="history-types">
          <button
            :class="['type-btn', { active: historyType === 'created' }]"
            @click="historyType = 'created'"
          >
            我发起的
          </button>
          <button
            :class="['type-btn', { active: historyType === 'joined' }]"
            @click="historyType = 'joined'"
          >
            我参与的
          </button>
        </div>
      </div>

      <div v-if="currentHistoryList.length" class="history-items">
        <div
          v-for="invite in currentHistoryList"
          :key="invite.id"
          class="history-card"
          @click="$router.push(`/invite/${invite.id}`)"
        >
          <div class="history-header">
            <div class="invite-type-badge" :style="{ background: getTypeColor(invite.inviteType) }">
              {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
            </div>
            <div class="invite-status-badge" :style="{ color: INVITE_STATUS_COLORS[invite.status as InviteStatus] }">
              {{ INVITE_STATUS_LABELS[invite.status as InviteStatus] }}
            </div>
          </div>
          <h3 class="history-title">{{ invite.title }}</h3>
          <div class="history-meta">
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatInviteTime(invite.inviteTime) }}
            </span>
            <span v-if="invite.location">
              <el-icon><Location /></el-icon>
              {{ invite.location }}
            </span>
            <span>
              <el-icon><UserFilled /></el-icon>
              {{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}人
            </span>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">
        <div class="empty-icon">📊</div>
        <p>当前时间范围内暂无记录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useInviteStore } from '@/store/inviteStore'
import { useUserStore } from '@/store/userStore'
import {
  cancelInviteWait,
  getMyInviteWaits,
  getRecommendInvites,
  getMyCreatedInvites,
  getMyJoinedInvites,
  type InviteWait,
  type Invite,
  type HistoryRange,
} from '@/api/inviteApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Clock, Location, UserFilled } from '@element-plus/icons-vue'
import {
  InviteType,
  InviteStatus,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
  INVITE_TYPE_OPTIONS,
  formatInviteTime,
} from '@/constants/inviteConst'

const inviteStore = useInviteStore()
const userStore = useUserStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="%23f0f2f5" width="40" height="40" rx="20"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="18">👤</text></svg>'

const activeTab = ref<string>('list')
const filterType = ref<string>()
const filterStatus = ref<string>()
const waitList = ref<InviteWait[]>([])
const recommendList = ref<Invite[]>([])
const createdHistory = ref<Invite[]>([])
const joinedHistory = ref<Invite[]>([])
const historyRange = ref<HistoryRange>('week')
const historyType = ref<'created' | 'joined'>('created')

const tabs = [
  { label: '我的邀约', value: 'list' },
  { label: '等待邀约', value: 'wait' },
  { label: '我的统计', value: 'history' },
]

const statusOptions = Object.entries(INVITE_STATUS_LABELS).map(([value, label]) => ({
  value,
  label,
}))

// 过滤后的邀约列表
const filteredInvites = computed(() => {
  let list = inviteStore.invites

  // 只展示当前用户发起的邀约
  const currentUserId = userStore.user?.id
  if (currentUserId) {
    list = list.filter(i => i.creatorId === currentUserId)
  }

  if (filterType.value) {
    list = list.filter(i => i.inviteType === filterType.value)
  }
  if (filterStatus.value) {
    list = list.filter(i => i.status === filterStatus.value)
  }

  return list
})

// 统计信息
const stats = computed(() => inviteStore.stats)

const historyRanges = [
  { label: '最近一周', value: 'week' as HistoryRange },
  { label: '最近一月', value: 'month' as HistoryRange },
  { label: '全部', value: 'all' as HistoryRange },
]

const currentHistoryList = computed(() => {
  return historyType.value === 'created' ? createdHistory.value : joinedHistory.value
})

async function loadHistory() {
  try {
    const [createdRes, joinedRes] = await Promise.all([
      getMyCreatedInvites(historyRange.value),
      getMyJoinedInvites(historyRange.value),
    ])
    createdHistory.value = createdRes.data.data || []
    joinedHistory.value = joinedRes.data.data || []
  } catch (error) {
    console.error('加载邀约历史失败:', error)
  }
}

function changeRange(range: HistoryRange) {
  if (historyRange.value === range) return
  historyRange.value = range
  loadHistory()
}

// 推荐邀约
async function loadRecommendInvites() {
  try {
    const res = await getRecommendInvites(6)
    recommendList.value = res.data.data || []
  } catch (error) {
    console.error('加载推荐邀约失败:', error)
  }
}

// 获取邀约类型颜色
function getTypeColor(type: string): string {
  const colors: Record<string, string> = {
    DINNER: '#ff6b9d',
    SPORT: '#52c41a',
    STUDY: '#1890ff',
    DRAMA: '#722ed1',
    OTHER: '#8c8c8c',
  }
  return colors[type] || '#8c8c8c'
}

// 格式化过期时间
function formatExpireTime(timeStr: string): string {
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 解析等待邀约类型展示为中文标签
function parseWaitTypes(inviteTypes: string): string[] {
  if (!inviteTypes) return []
  return inviteTypes.split(',')
    .map(t => t.trim())
    .filter(Boolean)
    .map(t => INVITE_TYPE_LABELS[t as InviteType] || t)
}

// 美化时间偏好展示（periodConfig 是 JSON 字符串）
function formatWaitPeriod(periodConfig: string | null): string {
  if (!periodConfig) return ''
  try {
    const obj = JSON.parse(periodConfig) as { start?: string; end?: string }
    if (!obj.start || !obj.end) return periodConfig
    const start = new Date(obj.start)
    const end = new Date(obj.end)
    const startStr = start.toLocaleString('zh-CN', {
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
    const endStr = end.toLocaleString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
    return `${startStr} ~ ${endStr}`
  } catch {
    return periodConfig
  }
}

// 筛选变化
function handleFilterChange() {
  inviteStore.fetchInvites(filterType.value, filterStatus.value)
}

// 取消等待邀约
async function handleCancelWait(id: number) {
  try {
    await ElMessageBox.confirm('确定取消这个等待邀约吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await cancelInviteWait(id)
    waitList.value = waitList.value.filter(w => w.id !== id)
    ElMessage.success('已取消等待邀约')
  } catch {
    // 用户取消操作
  }
}

// 加载等待邀约列表
async function loadWaitList() {
  try {
    const res = await getMyInviteWaits()
    waitList.value = res.data.data || []
  } catch (error) {
    console.error('加载等待邀约失败:', error)
  }
}

// 初始化
onMounted(() => {
  inviteStore.fetchInvites()
  inviteStore.fetchStats()
  loadWaitList()
  loadRecommendInvites()
  loadHistory()
})
</script>

<style lang="scss" scoped>
.invite-page { padding: 20px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
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

.filters-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  :deep(.el-select) {
    width: 140px;
  }
}

.invite-list, .wait-list, .history-list {
  min-height: 400px;
}

.recommend-section {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: $radius-lg;
  background: linear-gradient(135deg, rgba($primary, 0.05), rgba($primary, 0.02));
  border: 1px solid rgba($primary, 0.15);
}

.recommend-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.recommend-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.recommend-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.recommend-card {
  min-width: 220px;
  max-width: 260px;
  background: white;
  border-radius: $radius-lg;
  border: 1px solid $border-light;
  padding: 12px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary;
    box-shadow: $shadow-md;
  }
}

.recommend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.recommend-title-text {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.recommend-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: $text-secondary;
}

.loading-hint {
  text-align: center;
  padding: 60px;
  color: $text-muted;
}

.invite-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.invite-card {
  background: linear-gradient(135deg, rgba(#ff6b9d, 0.12), rgba(#c44569, 0.08));
  border: 1px solid rgba(#ff6b9d, 0.4);
  border-radius: $radius-lg;
  padding: 16px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary;
    box-shadow: $shadow-md;
  }
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
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
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.invite-content {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.5;
  margin-bottom: 12px;
}

.invite-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-secondary;
  }
}

.invite-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

.participants-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: $text-secondary;
}

.mode-info {
  font-size: 13px;
  color: $text-secondary;
}

.urgent-tag {
  padding: 2px 8px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 700;
}

.rating-info {
  margin-left: auto;
  font-size: 13px;
  color: $text-secondary;
}

.wait-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wait-card {
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border: 1px solid rgba($primary, 0.2);
  border-radius: $radius-lg;
  padding: 16px;
  box-shadow: $shadow-sm;
}

.wait-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.wait-types {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.wait-status-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

.wait-type-tag {
  padding: 2px 8px;
  border-radius: $radius-full;
  background: rgba($primary, 0.06);
  color: $primary;
  font-size: 12px;
  font-weight: 600;
}

.expired-tag {
  padding: 2px 8px;
  background: $border-light;
  color: $text-muted;
  border-radius: $radius-full;
  font-size: 12px;
}

.auto-accept-tag {
  padding: 2px 8px;
  background: rgba($success, 0.1);
  color: $success;
  border-radius: $radius-full;
  font-size: 12px;
  margin-left: 8px;
}

.pending-tag {
  padding: 2px 8px;
  background: rgba($warning, 0.1);
  color: $warning;
  border-radius: $radius-full;
  font-size: 12px;
}

.wait-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.wait-info {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.wait-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

.expire-time {
  font-size: 12px;
  color: $text-muted;
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
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: $primary;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
}

.history-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.history-range,
.history-types {
  display: flex;
  gap: 8px;
}

.range-btn,
.type-btn {
  padding: 6px 12px;
  border-radius: $radius-full;
  border: 1px solid $border-light;
  background: white;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &.active {
    background: $primary;
    border-color: $primary;
    color: white;
  }
}

.history-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-card {
  border-radius: $radius-lg;
  border: 1px solid $border-light;
  padding: 12px 14px;
  background: white;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary;
    box-shadow: $shadow-sm;
  }
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.history-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: $text-secondary;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
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
  padding: 4px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 13px;
  cursor: pointer;
  transition: color $transition-fast;

  &:hover {
    color: $primary;
  }

  &.danger:hover {
    color: $danger;
  }
}
</style>
