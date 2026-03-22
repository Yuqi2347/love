<template>
  <div class="invite-page">
    <div class="page-header">
      <h1 class="page-title">邀约</h1>
      <button class="btn-primary" @click="$router.push('/invite/create')">
        <el-icon><Plus /></el-icon> 发起邀约
      </button>
    </div>

    <AppTabBar v-model="activeTab" :tabs="tabs" />

    <div v-if="activeTab === 'list'" class="filters-row">
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

      <el-select
        v-model="filterTimeRange"
        placeholder="邀约时间"
        @change="handleFilterChange"
      >
        <el-option label="近一周内" value="week" />
        <el-option label="近一月内" value="month" />
        <el-option label="近一年内" value="year" />
      </el-select>

      <el-select
        v-model="inviteSource"
        placeholder="邀约来源"
        class="source-select"
        @change="handleSourceChange"
      >
        <el-option label="与我相关（含心动一对一）" value="related" />
        <el-option label="公共邀约" value="public" />
        <el-option label="我发起的" value="created" />
        <el-option label="我加入的" value="joined" />
      </el-select>
    </div>

    <!-- 列表工具栏：公共邀约可切换推荐/时间；与我相关仅刷新 -->
    <div v-if="activeTab === 'list' && (inviteSource === 'public' || inviteSource === 'related')" class="list-toolbar">
      <div v-if="inviteSource === 'public'" class="sort-pills">
        <button :class="['pill', { active: inviteSort === 'recommend' }]" @click="setInviteSort('recommend')">推荐</button>
        <button :class="['pill', { active: inviteSort === 'time' }]" @click="setInviteSort('time')">最新</button>
      </div>
      <p v-else class="list-toolbar-hint">含你发起、你加入及待处理的私密一对一（如心动协商生成）</p>
      <button class="btn-refresh-sm" :disabled="inviteStore.loading" @click="handleInviteRefresh">
        <el-icon :class="{ spinning: inviteStore.loading }"><Refresh /></el-icon>
        {{ inviteStore.loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <div v-if="activeTab === 'list'" class="invite-list">
      <div v-if="inviteStore.loading && !currentList.length" class="loading-hint">
        加载中...
      </div>
      <div v-else-if="currentList.length" class="invite-items">
        <div
          v-for="invite in filteredInvites"
          :key="invite.id"
          :class="['invite-card', { 'invite-card-1v1': invite.inviteMode === 'PRIVATE' }]"
          :style="invite.inviteMode !== 'PRIVATE' ? { borderLeft: `4px solid ${getTypeColor(invite.inviteType)}` } : {}"
          @click="$router.push(`/invite/${invite.id}`)"
        >
          <div class="invite-header">
            <div class="invite-header-badges">
              <div class="invite-type-badge" :style="{ background: getTypeColor(invite.inviteType) }">
                {{ INVITE_TYPE_EMOJI[invite.inviteType] || '' }} {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
              </div>
              <span v-if="invite.inviteMode === 'PRIVATE'" class="invite-badge-1v1">专属</span>
            </div>
            <div class="invite-status-badge" :style="{ color: getDisplayStatusColor(invite) }">
              {{ getDisplayStatusLabel(invite) }}
            </div>
          </div>

          <p v-if="invite.inviteMode === 'PRIVATE' && invite.myRole === 'TARGET_PENDING'" class="invite-1v1-from">
            {{ invite.creator?.nickname || 'TA' }} 邀你
          </p>
          <h3 class="invite-title">{{ invite.title }}</h3>
          <p v-if="invite.content" class="invite-content">{{ invite.content }}</p>

          <div class="invite-meta">
            <!-- 1v1 显示双方头像与昵称 -->
            <template v-if="invite.inviteMode === 'PRIVATE'">
              <div class="invite-1v1-users">
                <div class="invite-1v1-user" @click.stop="$router.push(`/profile/${invite.creatorId}`)">
                  <AppAvatar :src="invite.creator?.avatarUrl" :name="invite.creator?.nickname" :size="28" @click.stop="$router.push(`/profile/${invite.creatorId}`)" />
                  <span class="invite-1v1-name">{{ invite.creator?.nickname || '未知' }}</span>
                </div>
                <span class="invite-1v1-vs">↔</span>
                <div class="invite-1v1-user" @click.stop="$router.push(`/profile/${invite.targetUserId}`)">
                  <AppAvatar :src="invite.targetUser?.avatarUrl" :name="invite.targetUser?.nickname" :size="28" @click.stop="$router.push(`/profile/${invite.targetUserId}`)" />
                  <span class="invite-1v1-name">{{ invite.targetUser?.nickname || 'TA' }}</span>
                </div>
              </div>
            </template>
            <template v-else>
              <span class="meta-item meta-item-clickable" @click.stop="$router.push(`/profile/${invite.creatorId}`)">
                <el-icon><User /></el-icon>
                {{ invite.creator?.nickname || '未知' }}
              </span>
            </template>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatInviteTimeRange(invite.inviteTime, invite.inviteEndTime) }}
            </span>
            <span v-if="invite.location" class="meta-item">
              <el-icon><Location /></el-icon>
              {{ invite.location }}
            </span>
          </div>

          <div class="invite-footer">
            <div class="participants-info">
              <el-icon><UserFilled /></el-icon>
              <span>{{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}人</span>
              <div v-if="invite.maxParticipants && invite.inviteMode !== 'PRIVATE'" class="participant-bar">
                <div class="participant-bar-fill" :style="{ width: `${Math.min(100, (invite.participantCount / invite.maxParticipants) * 100)}%` }" />
              </div>
            </div>
            <div v-if="invite.isUrgent" class="urgent-tag pulse">急需</div>
            <div v-if="invite.ratingCount" class="rating-info">
              ⭐ {{ invite.socialRating?.toFixed(1) || '-' }} ({{ invite.ratingCount }})
            </div>
            <!-- 待处理邀约：同意/拒绝 -->
            <div v-if="invite.myRole === 'TARGET_PENDING'" class="invite-pending-actions" @click.stop>
              <el-button type="primary" size="small" @click="handleAcceptInviteCard(invite.id)">同意</el-button>
              <el-button size="small" @click="handleDeclineInviteCard(invite.id)">拒绝</el-button>
            </div>
          </div>
        </div>
      </div>
      <AppEmptyState v-else icon="📅" text="暂无邀约，快发起一个或去发现里加入吧" />
      <!-- 加载更多 sentinel（在 v-if 链之外，始终渲染） -->
      <div ref="inviteSentinelRef" class="sentinel">
        <div v-if="inviteStore.loading && currentList.length" class="loading-more">
          <el-icon class="spinning"><Loading /></el-icon> 加载中...
        </div>
        <div v-else-if="!inviteStore.inviteHasMore && currentList.length && inviteSource === 'public'" class="no-more">
          已加载全部
        </div>
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
      <AppEmptyState v-else icon="⏳" text="暂无等待邀约">
        <button class="btn-outline" @click="$router.push('/invite/wait')">创建等待邀约</button>
      </AppEmptyState>
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
              {{ INVITE_TYPE_EMOJI[invite.inviteType] || '' }} {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
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
      <AppEmptyState v-else icon="📊" text="当前时间范围内暂无记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useInviteStore } from '@/store/inviteStore'
import { useBadgeStore } from '@/store/badgeStore'
import {
  cancelInviteWait,
  getMyInviteWaits,
  getMyCreatedInvites,
  getMyJoinedInvites,
  joinInvite,
  declineInvite,
  type InviteWait,
  type Invite,
  type HistoryRange,
} from '@/api/inviteApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Clock, Location, UserFilled, Refresh, Loading } from '@element-plus/icons-vue'
import {
  InviteType,
  InviteStatus,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
  INVITE_TYPE_OPTIONS,
  formatInviteTime,
  formatInviteTimeRange,
} from '@/constants/inviteConst'
import { getTypeColor } from '@/utils/shared'
import AppTabBar from '@/components/AppTabBar.vue'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { INVITE_TYPE_EMOJI } from '@/constants/emojiConst'

const router = useRouter()
const route = useRoute()
const inviteStore = useInviteStore()
const badgeStore = useBadgeStore()

const activeTab = ref<string>('list')
const inviteSource = ref<'related' | 'public' | 'created' | 'joined'>('related')
const inviteSort = ref<'recommend' | 'time'>('recommend')
const filterType = ref<string>()
const filterStatus = ref<string>()
const filterTimeRange = ref<string>('week')
const waitList = ref<InviteWait[]>([])
const createdHistory = ref<Invite[]>([])
const joinedHistory = ref<Invite[]>([])
const historyRange = ref<HistoryRange>('week')
const historyType = ref<'created' | 'joined'>('created')

// 加载更多 sentinel
const inviteSentinelRef = ref<HTMLElement>()
let inviteObserver: IntersectionObserver | null = null

// 手机端下拉刷新
let iTouchStartY = 0

function setInviteSort(sort: 'recommend' | 'time') {
  inviteSort.value = sort
  loadInvitesBySource()
}

function handleInviteRefresh() {
  loadInvitesBySource()
}

const tabs = [
  { label: '邀约', value: 'list' },
  { label: '等待被邀约', value: 'wait' },
  { label: '我的统计', value: 'history' },
]

const statusOptions = [
  ...Object.entries(INVITE_STATUS_LABELS).map(([value, label]) => ({ value, label })),
  { value: 'LEFT', label: '已退出' },
  { value: 'TARGET_PENDING', label: '待处理' },
]

// 当前来源对应的原始列表
const currentList = computed(() => {
  if (inviteSource.value === 'related') return inviteStore.myListInvites
  if (inviteSource.value === 'public') return inviteStore.invites
  if (inviteSource.value === 'created') return inviteStore.createdInvites
  return inviteStore.joinedInvites
})

// 过滤后的邀约列表（顺序由后端保证：状态优先级 + 同状态内按发布时间）
const filteredInvites = computed(() => {
  let list = currentList.value

  if (filterType.value) {
    list = list.filter(i => i.inviteType === filterType.value)
  }
  if (filterStatus.value) {
    list = list.filter(i => {
      if (i.myRole === 'TARGET_PENDING') return filterStatus.value === 'TARGET_PENDING'
      if (i.myRole === 'LEFT') return filterStatus.value === 'LEFT'
      return i.status === filterStatus.value
    })
  }

  return list.slice()
})

// 列表展示用状态文案（已退出优先）
function getDisplayStatusLabel(invite: Invite) {
  if (invite.myRole === 'TARGET_PENDING') return '待处理'
  if (invite.myRole === 'LEFT') return '已退出'
  if (invite.inviteMode === 'PRIVATE' && invite.status === 'FULL' && invite.myRole === 'PARTICIPANT') return '已成行'
  return INVITE_STATUS_LABELS[invite.status as InviteStatus] ?? invite.status
}

function getDisplayStatusColor(invite: Invite) {
  if (invite.myRole === 'TARGET_PENDING') return '#e6a23c'
  if (invite.myRole === 'LEFT') return '#8c8c8c'
  return INVITE_STATUS_COLORS[invite.status as InviteStatus] ?? '#8c8c8c'
}

async function handleAcceptInviteCard(inviteId: number) {
  try {
    await joinInvite(inviteId)
    ElMessage.success('已接受邀约')
    await loadInvitesBySource()
    await router.push(`/invite/${inviteId}`)
  } catch (e: any) {
    const msg = e?.response?.data?.message ?? e?.message ?? '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDeclineInviteCard(inviteId: number) {
  try {
    await declineInvite(inviteId)
    ElMessage.success('已拒绝')
    await loadInvitesBySource()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

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

// 筛选变化（我的邀约列表用 my-list 接口，带时间范围）
function handleFilterChange() {
  loadInvitesBySource()
}

// 邀约来源切换
function handleSourceChange() {
  loadInvitesBySource()
}

// 根据来源加载邀约（刷新：清空重拉）
async function loadInvitesBySource() {
  if (inviteSource.value === 'public') {
    await inviteStore.fetchInvites(filterType.value, filterStatus.value, filterTimeRange.value, undefined, true, inviteSort.value)
  } else if (inviteSource.value === 'related') {
    const range = filterTimeRange.value === 'year' ? 'all' : filterTimeRange.value || 'week'
    await inviteStore.fetchMyInvitesList(range)
  } else if (inviteSource.value === 'created') {
    await inviteStore.fetchCreatedInvites(filterTimeRange.value)
  } else {
    await inviteStore.fetchJoinedInvites(filterTimeRange.value)
  }
}

// 加载更多公共邀约
async function loadMorePublicInvites() {
  if (inviteSource.value !== 'public') return
  if (!inviteStore.inviteHasMore || inviteStore.loading) return
  await inviteStore.loadMoreInvites(undefined, true)
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

// 从路由 query 初始化筛选（如热门看板点击跳转）
function initFromRouteQuery() {
  const type = route.query.type as string | undefined
  const source = route.query.source as string | undefined
  if (type) filterType.value = type
  if (source && (source === 'related' || source === 'public' || source === 'created' || source === 'joined')) {
    inviteSource.value = source as 'related' | 'public' | 'created' | 'joined'
  }
}

// 初始化（进入页即标记活动已查看）
onMounted(() => {
  badgeStore.markInviteActivityViewed()
  initFromRouteQuery()
  loadInvitesBySource()
  inviteStore.fetchStats()
  loadWaitList()
  loadHistory()

  // IntersectionObserver：上划触底加载更多
  inviteObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && inviteSource.value === 'public') {
        loadMorePublicInvites()
      }
    },
    { rootMargin: '120px' }
  )
  if (inviteSentinelRef.value) inviteObserver.observe(inviteSentinelRef.value)

  // 手机端下拉刷新
  document.addEventListener('touchstart', onInviteTouchStart, { passive: true })
  document.addEventListener('touchend', onInviteTouchEnd, { passive: true })
})

onUnmounted(() => {
  inviteObserver?.disconnect()
  document.removeEventListener('touchstart', onInviteTouchStart)
  document.removeEventListener('touchend', onInviteTouchEnd)
})

function onInviteTouchStart(e: TouchEvent) {
  iTouchStartY = e.touches[0]?.clientY ?? 0
}

function onInviteTouchEnd(e: TouchEvent) {
  if (!iTouchStartY) return
  const endY = e.changedTouches[0]?.clientY ?? 0
  const delta = endY - iTouchStartY
  if (delta > 80 && window.scrollY < 10 && !inviteStore.loading) {
    loadInvitesBySource()
  }
  iTouchStartY = 0
}

watch(() => [route.query.type, route.query.source], () => {
  initFromRouteQuery()
  loadInvitesBySource()
})
</script>

<style lang="scss" scoped>
.invite-page { padding: 0 20px 20px 20px; }

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}

.list-toolbar-hint {
  flex: 1;
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: #64748b;
}

.sort-pills {
  display: flex;
  gap: 6px;
}

.pill {
  padding: 4px 14px;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { color: $primary; border-color: $primary; }
  &.active { background: $primary; color: white; border-color: $primary; }
}

.btn-refresh-sm {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover:not(:disabled) { color: $primary; border-color: $primary; }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.sentinel {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 4px 0;
}

.loading-more, .no-more {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-muted;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning { animation: spin 1s linear infinite; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  position: sticky;
  top: 0;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  z-index: 10;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
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
  background: $bg-primary;
  border: none;
  border-radius: $radius-xl;
  padding: 20px;
  cursor: pointer;
  box-shadow: $shadow-sm;
  transition: transform $transition-fast, box-shadow $transition-fast;
  contain: content;
  animation: card-enter 0.35s ease both;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
  }

  &.invite-card-1v1 {
    background: linear-gradient(135deg, $bg-primary, rgba(#9c27b0, 0.03));
    border-left: 4px solid #9c27b0;
  }
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.invite-header-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.invite-badge-1v1 {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #7b1fa2;
  background: rgba(#9c27b0, 0.15);
  border-radius: $radius-full;
}

.invite-1v1-from {
  font-size: 13px;
  color: #7b1fa2;
  margin: -4px 0 6px 0;
  font-weight: 500;
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
  line-height: 1.55;
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.invite-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;

  .invite-1v1-users {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .invite-1v1-user {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;

    &:hover .invite-1v1-name { color: $primary; }
  }
  .invite-1v1-avatar {
    border-radius: 50%;
    object-fit: cover;
  }
  .invite-1v1-name {
    font-size: 13px;
    color: $text-secondary;
  }
  .invite-1v1-vs {
    font-size: 12px;
    color: $text-muted;
  }
  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-secondary;
  }

  .meta-item-clickable {
    cursor: pointer;

    &:hover { color: $primary; }
  }
}

.invite-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

.invite-pending-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.participants-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: $text-secondary;
}

.participant-bar {
  width: 60px;
  height: 4px;
  background: $bg-tertiary;
  border-radius: 2px;
  overflow: hidden;
  margin-left: 4px;
}

.participant-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, $primary, #ff6b9d);
  border-radius: 2px;
  transition: width $transition-base;
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

  &.pulse {
    animation: pulse-glow 2s ease-in-out infinite;
  }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(#ff6b6b, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(#ff6b6b, 0); }
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

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@for $i from 1 through 10 {
  .invite-card:nth-child(#{$i}) {
    animation-delay: #{$i * 0.05}s;
  }
}

@media (max-width: $bp-mobile) {
  .invite-page { padding: 0 12px 12px; }
  .filters-row { flex-wrap: wrap; }
  .filters-row :deep(.el-select) { width: calc(50% - 6px); }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .history-filters { flex-direction: column; gap: 8px; }
}
</style>
