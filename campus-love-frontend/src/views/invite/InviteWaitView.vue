<template>
<div class="invite-page">
  <div class="page-header glass-panel">
    <h1 class="page-title text-gradient-warm">同行邀约</h1>
    <button class="glow-btn-warm px-4" @click="$router.push('/invite/create')">
      <el-icon><Plus /></el-icon> 发起邀约
    </button>
  </div>

  <div class="tuner-capsule glass-panel mt-4 mb-4">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      class="tuner-btn"
      :class="{ active: activeTab === tab.value }"
      @click="activeTab = tab.value"
    >
      {{ tab.label }}
    </button>
  </div>

  <div v-if="activeTab === 'list'" class="tab-content">
    <div class="filters-row glass-panel p-3">
      <el-select v-model="filterType" placeholder="邀约类型" clearable class="glass-select" @change="handleFilterChange">
        <el-option v-for="opt in INVITE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="邀约状态" clearable class="glass-select" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filterTimeRange" placeholder="邀约时间" class="glass-select" @change="handleFilterChange">
        <el-option label="近一周内" value="week" />
        <el-option label="近一月内" value="month" />
        <el-option label="近一年内" value="year" />
      </el-select>
      <el-select v-model="inviteSource" placeholder="邀约来源" class="glass-select source-select" @change="handleSourceChange">
        <el-option label="公共邀约" value="public" />
        <el-option label="与我相关（含一对一）" value="related" />
        <el-option label="我发起的" value="created" />
        <el-option label="我加入的" value="joined" />
      </el-select>
    </div>

    <div v-if="inviteSource === 'public' || inviteSource === 'related'" class="list-toolbar">
      <div v-if="inviteSource === 'public'" class="sort-pills glass-pill-light">
        <button :class="['pill', { active: inviteSort === 'recommend' }]" @click="setInviteSort('recommend')">推荐</button>
        <button :class="['pill', { active: inviteSort === 'time' }]" @click="setInviteSort('time')">最新</button>
      </div>
      <p v-else class="list-toolbar-hint glass-pill-light px-3 py-1 m-0">含你发起、加入及待处理的心动私密邀约</p>
      
      <button class="glass-btn btn-refresh-sm" :disabled="inviteStore.loading" @click="handleInviteRefresh">
        <el-icon :class="{ spinning: inviteStore.loading }"><Refresh /></el-icon>
        {{ inviteStore.loading ? '刷新中...' : '刷新星轨' }}
      </button>
    </div>

    <p class="invite-policy-hint glass-pill-light">
      当前频段承载上限：公共 {{ concurrentPublicLimit }} 场，私密 {{ concurrentPrivateLimit }} 场
    </p>

    <div class="invite-list">
      <div v-if="inviteStore.loading && !currentList.length" class="loading-hint">
        <div class="pulse-ring mx-auto mb-2"></div>
        感知周边邀约信号中...
      </div>
      
      <div v-else-if="currentList.length" class="invite-items">
        <div
          v-for="invite in filteredInvites"
          :key="invite.id"
          class="invite-card glass-card-light"
          :class="{ 'invite-card-1v1': invite.inviteMode === 'PRIVATE' }"
          @click="$router.push(`/invite/${invite.id}`)"
        >
          <div v-if="invite.inviteMode !== 'PRIVATE'" class="card-glow-indicator" :style="{ background: getTypeColor(invite.inviteType) }"></div>

          <div class="invite-header">
            <div class="invite-header-badges">
              <div class="invite-type-badge text-shadow-sm" :style="{ background: getTypeColor(invite.inviteType) }">
                {{ INVITE_TYPE_EMOJI[invite.inviteType] || '' }} {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
              </div>
              <span v-if="invite.inviteMode === 'PRIVATE'" class="invite-badge-1v1 pulse-border">心动专属</span>
            </div>
            <div class="invite-status-badge glass-pill px-3 py-1" :style="{ color: getDisplayStatusColor(invite) }">
              {{ getDisplayStatusLabel(invite) }}
            </div>
          </div>

          <p v-if="invite.inviteMode === 'PRIVATE' && invite.myRole === 'TARGET_PENDING'" class="invite-1v1-from text-accent-pink font-bold">
            <span class="pulse-dot"></span> {{ invite.creator?.nickname || 'TA' }} 向你发送了专属邀请
          </p>
          <h3 class="invite-title text-main">{{ invite.title }}</h3>
          <p v-if="invite.content" class="invite-content">{{ invite.content }}</p>

          <div class="invite-meta glass-pill-light">
            <template v-if="invite.inviteMode === 'PRIVATE'">
              <div class="invite-1v1-users">
                <div class="invite-1v1-user" @click.stop="$router.push(`/profile/${invite.creatorId}`)">
                  <AppAvatar :src="invite.creator?.avatarUrl" :name="invite.creator?.nickname" :size="28" class="avatar-sm" />
                  <span class="invite-1v1-name">{{ invite.creator?.nickname || '未知' }}</span>
                </div>
                <span class="invite-1v1-vs">⟷</span>
                <div class="invite-1v1-user" @click.stop="$router.push(`/profile/${invite.targetUserId}`)">
                  <AppAvatar :src="invite.targetUser?.avatarUrl" :name="invite.targetUser?.nickname" :size="28" class="avatar-sm" />
                  <span class="invite-1v1-name">{{ invite.targetUser?.nickname || 'TA' }}</span>
                </div>
              </div>
            </template>
            <template v-else>
              <span class="meta-item meta-item-clickable" @click.stop="$router.push(`/profile/${invite.creatorId}`)">
                <el-icon><User /></el-icon> {{ invite.creator?.nickname || '未知' }}
              </span>
            </template>
            <span class="meta-item"><el-icon><Clock /></el-icon> {{ formatInviteTimeRange(invite.inviteTime, invite.inviteEndTime) }}</span>
            <span v-if="invite.location" class="meta-item"><el-icon><Location /></el-icon> {{ invite.location }}</span>
          </div>

          <div class="invite-footer">
            <div class="participants-info">
              <el-icon><UserFilled /></el-icon>
              <span>{{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}人</span>
              <div v-if="invite.maxParticipants && invite.inviteMode !== 'PRIVATE'" class="participant-bar">
                <div class="participant-bar-fill glow-bg-warm" :style="{ width: `${Math.min(100, (invite.participantCount / invite.maxParticipants) * 100)}%` }" />
              </div>
            </div>
            <div v-if="invite.isUrgent" class="urgent-tag pulse-shadow">急需同行</div>
            <div v-if="invite.ratingCount" class="rating-info glass-pill px-2">⭐ {{ invite.socialRating?.toFixed(1) || '-' }} ({{ invite.ratingCount }})</div>
            
            <div v-if="invite.myRole === 'TARGET_PENDING'" class="invite-pending-actions" @click.stop>
              <button class="glow-btn-warm px-4 py-1 text-sm h-8" @click="handleAcceptInviteCard(invite.id)">接受</button>
              <button class="glass-btn px-4 py-1 text-sm h-8" @click="handleDeclineInviteCard(invite.id)">婉拒</button>
            </div>
          </div>
        </div>
      </div>
      <AppEmptyState v-else icon="📅" text="引力场内暂无符合条件的邀约" />

      <div ref="inviteSentinelRef" class="sentinel mt-4">
        <div v-if="inviteStore.loading && currentList.length" class="loading-more">
          <el-icon class="spinning"><Loading /></el-icon> 探索深层信号...
        </div>
        <div v-else-if="!inviteStore.inviteHasMore && currentList.length && inviteSource === 'public'" class="no-more">
          已抵达星轨尽头
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="activeTab === 'wait'" class="wait-list tab-content">
    <div class="wait-header-row">
      <h2 class="wait-section-title">
        <el-icon class="text-accent-blue"><Connection /></el-icon>
        运行中的引力雷达
      </h2>
      <button class="glow-btn-warm px-4 h-10" @click="$router.push('/invite/wait')">
        <el-icon><Plus /></el-icon> 开启新雷达
      </button>
    </div>

    <div v-if="waitList.length" class="wait-items">
      <div v-for="wait in waitList" :key="wait.id" class="wait-card glass-card-light">
        
        <div class="wait-card-top">
          <div class="wait-types">
            <span v-for="type in parseWaitTypes(wait.inviteTypes)" :key="type" class="wait-type-tag glass-pill">
              {{ type }}
            </span>
          </div>
          <div class="wait-status-tags">
            <span v-if="wait.isExpired" class="expired-tag glass-pill">已过期</span>
            <span v-else class="pending-tag glass-pill pulse-border">雷达扫描中</span>
            <span v-if="wait.autoAccept" class="auto-accept-tag glass-pill">自动上车</span>
          </div>
        </div>

        <div class="wait-card-middle">
          <div v-if="wait.periodConfig" class="wait-info-row">
            <el-icon class="text-accent-blue"><Clock /></el-icon>
            <span class="info-label">时段预期：</span>
            <span class="info-value">{{ formatWaitPeriod(wait.periodConfig) }}</span>
          </div>
          <div v-if="wait.locationPref" class="wait-info-row">
            <el-icon class="text-accent-pink"><Location /></el-icon>
            <span class="info-label">地点预期：</span>
            <span class="info-value">{{ wait.locationPref }}</span>
          </div>
        </div>
        
        <div class="wait-card-bottom">
          <span class="expire-time"><el-icon><Timer /></el-icon> 有效期至：{{ formatExpireTime(wait.expireTime) }}</span>
          <button class="btn-cancel-wait" @click="handleCancelWait(wait.id)">关闭雷达</button>
        </div>
      </div>
    </div>
    
    <AppEmptyState v-else icon="⏳" text="雷达阵列静默中，开启扫描让别人找到你">
      <button class="glow-btn-warm px-6 mt-4" @click="$router.push('/invite/wait')">开启等待雷达</button>
    </AppEmptyState>
  </div>

  <div v-else-if="activeTab === 'history'" class="history-list tab-content">
    <div v-if="stats" class="stats-card glass-panel">
      <h3 class="stats-title"><span class="title-accent"></span>引力场活跃数据</h3>
      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-value text-gradient-warm">{{ stats.inviteCount }}</span>
          <span class="stat-label">发射信号</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-gradient-warm">{{ stats.participateCount }}</span>
          <span class="stat-label">成功接轨</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-gradient-warm">{{ stats.successRate.toFixed(1) }}%</span>
          <span class="stat-label">成行率</span>
        </div>
        <div class="stat-item">
          <span class="stat-value text-gradient-warm">{{ stats.receivedSocialRating?.toFixed(1) || '-' }}</span>
          <span class="stat-label">社交引力值</span>
        </div>
      </div>
    </div>

    <div class="history-filters glass-panel">
      <div class="history-range">
        <button v-for="r in historyRanges" :key="r.value" :class="['range-btn tuner-btn', { active: historyRange === r.value }]" @click="changeRange(r.value)">
          {{ r.label }}
        </button>
      </div>
      <div class="history-types">
        <button :class="['type-btn tuner-btn', { active: historyType === 'created' }]" @click="historyType = 'created'">我发起的</button>
        <button :class="['type-btn tuner-btn', { active: historyType === 'joined' }]" @click="historyType = 'joined'">我加入的</button>
      </div>
    </div>

    <div v-if="currentHistoryList.length" class="history-items">
      <div v-for="invite in currentHistoryList" :key="invite.id" class="history-card glass-card-light" @click="$router.push(`/invite/${invite.id}`)">
        <div class="history-card-top">
          <div class="invite-type-badge text-shadow-sm" :style="{ background: getTypeColor(invite.inviteType) }">
            {{ INVITE_TYPE_EMOJI[invite.inviteType] || '' }} {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
          </div>
          <div class="invite-status-badge" :style="{ color: INVITE_STATUS_COLORS[invite.status as InviteStatus] }">
            {{ INVITE_STATUS_LABELS[invite.status as InviteStatus] }}
          </div>
        </div>
        <h3 class="history-title">{{ invite.title }}</h3>
        <div class="history-meta glass-pill-light">
          <span class="meta-item"><el-icon><Clock /></el-icon> {{ formatInviteTime(invite.inviteTime) }}</span>
          <span v-if="invite.location" class="meta-item"><el-icon><Location /></el-icon> {{ invite.location }}</span>
          <span class="meta-item"><el-icon><UserFilled /></el-icon> {{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}人</span>
        </div>
      </div>
    </div>
    <AppEmptyState v-else icon="📊" text="当前时空范围内暂无记录" />
  </div>
</div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
defineOptions({ name: 'Invite' })
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useInviteStore } from '@/store/inviteStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useUserStore } from '@/store/userStore'
import {
cancelInviteWait, getMyInviteWaits, getMyCreatedInvites, getMyJoinedInvites,
joinInvite, declineInvite, type InviteWait, type Invite, type HistoryRange,
} from '@/api/inviteApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Clock, Location, UserFilled, Refresh, Loading, Connection, Timer } from '@element-plus/icons-vue'
import {
InviteType, InviteStatus, INVITE_TYPE_LABELS, INVITE_STATUS_LABELS,
INVITE_STATUS_COLORS, INVITE_TYPE_OPTIONS, formatInviteTime, formatInviteTimeRange,
} from '@/constants/inviteConst'
import { getTypeColor } from '@/utils/shared'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { INVITE_TYPE_EMOJI } from '@/constants/emojiConst'

const router = useRouter()
const route = useRoute()
const inviteStore = useInviteStore()
const badgeStore = useBadgeStore()
const userStore = useUserStore()

const activeTab = ref<string>('list')
const inviteSource = ref<'related' | 'public' | 'created' | 'joined'>('public')
const inviteSort = ref<'recommend' | 'time'>('recommend')
const filterType = ref<string>()
const filterStatus = ref<string>()
const filterTimeRange = ref<string>('week')
const waitList = ref<InviteWait[]>([])
const createdHistory = ref<Invite[]>([])
const joinedHistory = ref<Invite[]>([])
const historyRange = ref<HistoryRange>('week')
const historyType = ref<'created' | 'joined'>('created')

const inviteSentinelRef = ref<HTMLElement>()
let inviteObserver: IntersectionObserver | null = null
let inviteSideEffectsBound = false
let iTouchStartY = 0

function setInviteSort(sort: 'recommend' | 'time') { inviteSort.value = sort; loadInvitesBySource() }
function handleInviteRefresh() { loadInvitesBySource() }

const tabs = [
{ label: '探索邀约', value: 'list' },
{ label: '我的等待雷达', value: 'wait' },
{ label: '引力统计', value: 'history' },
]

const statusOptions = [
...Object.entries(INVITE_STATUS_LABELS).map(([value, label]) => ({ value, label })),
{ value: 'LEFT', label: '已退出' },
{ value: 'TARGET_PENDING', label: '待处理' },
]

const currentList = computed(() => {
if (inviteSource.value === 'related') return inviteStore.myListInvites
if (inviteSource.value === 'public') return inviteStore.invites
if (inviteSource.value === 'created') return inviteStore.createdInvites
return inviteStore.joinedInvites
})

const filteredInvites = computed(() => {
let list = currentList.value
if (filterType.value) list = list.filter(i => i.inviteType === filterType.value)
if (filterStatus.value) {
  list = list.filter(i => {
    if (i.myRole === 'TARGET_PENDING') return filterStatus.value === 'TARGET_PENDING'
    if (i.myRole === 'LEFT') return filterStatus.value === 'LEFT'
    return i.status === filterStatus.value
  })
}
return list.slice()
})

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
  await joinInvite(inviteId); ElMessage.success('已接受邀约'); await loadInvitesBySource(); await router.push(`/invite/${inviteId}`)
} catch (e: any) { ElMessage.error(e?.response?.data?.message ?? e?.message ?? '操作失败') }
}

async function handleDeclineInviteCard(inviteId: number) {
try { await declineInvite(inviteId); ElMessage.success('已拒绝'); await loadInvitesBySource() } 
catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

const stats = computed(() => inviteStore.stats)
const historyRanges = [{ label: '最近一周', value: 'week' as HistoryRange }, { label: '最近一月', value: 'month' as HistoryRange }, { label: '全部', value: 'all' as HistoryRange }]
const currentHistoryList = computed(() => historyType.value === 'created' ? createdHistory.value : joinedHistory.value)

const concurrentPublicLimit = computed(() => {
const f = userStore.user?.invitePublicConcurrentLimit; if (typeof f === 'number' && f > 0) return f
const lvl = Number(userStore.user?.userLevel || 1); return lvl <= 3 ? 1 : lvl <= 5 ? 2 : 3
})
const concurrentPrivateLimit = computed(() => {
const f = userStore.user?.invitePrivateConcurrentLimit; if (typeof f === 'number' && f > 0) return f
const lvl = Number(userStore.user?.userLevel || 1); return lvl <= 3 ? 1 : lvl <= 5 ? 2 : 3
})

async function loadHistory() {
try {
  const [c, j] = await Promise.all([getMyCreatedInvites(historyRange.value), getMyJoinedInvites(historyRange.value)])
  createdHistory.value = c.data.data || []; joinedHistory.value = j.data.data || []
} catch {}
}
function changeRange(range: HistoryRange) { if (historyRange.value !== range) { historyRange.value = range; loadHistory() } }
function formatExpireTime(t: string) { return new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }
function parseWaitTypes(t: string) { return !t ? [] : t.split(',').map(x => x.trim()).filter(Boolean).map(x => INVITE_TYPE_LABELS[x as InviteType] || x) }
function formatWaitPeriod(pc: string | null) {
if (!pc) return ''
try {
  const o = JSON.parse(pc); if (!o.start || !o.end) return pc
  return `${new Date(o.start).toLocaleString('zh-CN', { month:'numeric',day:'numeric',hour:'2-digit',minute:'2-digit' })} ~ ${new Date(o.end).toLocaleString('zh-CN', { hour:'2-digit',minute:'2-digit'})}`
} catch { return pc }
}

function handleFilterChange() { loadInvitesBySource() }
function handleSourceChange() { loadInvitesBySource() }

async function loadInvitesBySource() {
if (inviteSource.value === 'public') await inviteStore.fetchInvites(filterType.value, filterStatus.value, filterTimeRange.value, undefined, true, inviteSort.value)
else if (inviteSource.value === 'related') await inviteStore.fetchMyInvitesList(filterTimeRange.value === 'year' ? 'all' : filterTimeRange.value || 'week')
else if (inviteSource.value === 'created') await inviteStore.fetchCreatedInvites(filterTimeRange.value)
else await inviteStore.fetchJoinedInvites(filterTimeRange.value)
}

async function loadMorePublicInvites() { if (inviteSource.value === 'public' && inviteStore.inviteHasMore && !inviteStore.loading) await inviteStore.loadMoreInvites(undefined, true) }

async function handleCancelWait(id: number) {
try {
  await ElMessageBox.confirm('确定关闭该等待雷达吗？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await cancelInviteWait(id); waitList.value = waitList.value.filter(w => w.id !== id); ElMessage.success('雷达已关闭')
} catch {}
}

async function loadWaitList() { try { waitList.value = (await getMyInviteWaits()).data.data || [] } catch {} }

function initFromRouteQuery() {
const t = route.query.type as string | undefined; const s = route.query.source as string | undefined
if (t) filterType.value = t
if (s && ['related', 'public', 'created', 'joined'].includes(s)) inviteSource.value = s as any
}

onMounted(() => {
badgeStore.markInviteActivityViewed(); initFromRouteQuery(); loadInvitesBySource(); inviteStore.fetchStats(); loadWaitList(); loadHistory()
bindInviteSideEffects()
})
onUnmounted(() => unbindInviteSideEffects())
onActivated(() => bindInviteSideEffects())
onDeactivated(() => unbindInviteSideEffects())

function bindInviteSideEffects() {
if (inviteSideEffectsBound) return; inviteSideEffectsBound = true
inviteObserver = new IntersectionObserver((e) => { if (e[0]?.isIntersecting && inviteSource.value === 'public') loadMorePublicInvites() }, { rootMargin: '120px' })
if (inviteSentinelRef.value) inviteObserver.observe(inviteSentinelRef.value)
document.addEventListener('touchstart', onInviteTouchStart, { passive: true }); document.addEventListener('touchend', onInviteTouchEnd, { passive: true })
}
function unbindInviteSideEffects() {
if (!inviteSideEffectsBound) return; inviteSideEffectsBound = false
inviteObserver?.disconnect(); inviteObserver = null
document.removeEventListener('touchstart', onInviteTouchStart); document.removeEventListener('touchend', onInviteTouchEnd)
}
function onInviteTouchStart(e: TouchEvent) { iTouchStartY = e.touches[0]?.clientY ?? 0 }
function onInviteTouchEnd(e: TouchEvent) {
if (!iTouchStartY) return
if ((e.changedTouches[0]?.clientY ?? 0) - iTouchStartY > 80 && window.scrollY < 10 && !inviteStore.loading) loadInvitesBySource()
iTouchStartY = 0
}
watch(() => [route.query.type, route.query.source], () => { initFromRouteQuery(); loadInvitesBySource() })
</script>

<style lang="scss" scoped>
/* ==========================================
  晨曦极光 (Light Glassmorphism) 列表页 UI
  ========================================== */
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;

.invite-page { padding: 0 0 24px 0; max-width: 800px; margin: 0 auto; position: relative;}

/* --- 极光玻璃态核心工具类 --- */
.glass-panel { background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); border: 1px solid rgba(255,255,255,0.8); box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05); border-radius: 24px; }
.glass-card-light { background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 20px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02); }
.glass-pill { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px; }
.glass-pill-light { background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px; }

.text-gradient-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800; }
.glow-btn-warm { height: 42px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center; gap: 6px; background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; font-size: 15px; font-weight: 700; cursor: pointer; box-shadow: 0 8px 20px rgba(255, 51, 102, 0.3); transition: all 0.3s; &:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(255, 51, 102, 0.4); } }
.glass-btn { height: 36px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff; color: $text-sub; font-size: 13px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; transition: all 0.3s; &:hover { background: #fff; color: $text-main; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); } }

// 动画
.pulse-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: $accent-pink; animation: pulse-shadow 2s infinite; margin-right: 4px; }
.pulse-shadow { animation: pulse-shadow 2s infinite; }
.pulse-border { animation: pulse-border 2s infinite; border: 1px solid $accent-pink; }
.pulse-ring { width: 40px; height: 40px; border-radius: 50%; border: 3px solid rgba(79,140,255,0.2); border-top-color: $accent-blue; animation: spin 1s linear infinite; }
.spinning { animation: spin 1s linear infinite; }
.panel-entrance { animation: rise-in 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
.tab-content { animation: fade-in 0.4s ease; }

@keyframes spin { 100% { transform: rotate(360deg); } }
@keyframes pulse-shadow { 0% { box-shadow: 0 0 0 0 rgba(255,51,102, 0.4); } 70% { box-shadow: 0 0 0 6px rgba(255,51,102, 0); } 100% { box-shadow: 0 0 0 0 rgba(255,51,102, 0); } }
@keyframes pulse-border { 0% { box-shadow: 0 0 0 0 rgba(255,51,102, 0.2); } 50% { box-shadow: 0 0 0 4px rgba(255,51,102, 0); } 100% { box-shadow: 0 0 0 0 rgba(255,51,102, 0); } }
@keyframes rise-in { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }

/* ================= 头部与导航 ================= */
.page-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; position: sticky; top: 12px; z-index: 50; margin-bottom: 16px;}
.page-title { font-size: 26px; margin: 0; font-family: $serif; letter-spacing: 1px;}

// 胶囊导航 Tabs
.tuner-capsule { display: flex; padding: 6px; gap: 4px; }
.tuner-btn {
flex: 1; padding: 10px 0; border-radius: 999px; text-align: center; font-size: 14px; font-weight: 700;
color: $text-sub; transition: all 0.3s; border: none; background: transparent; cursor: pointer;
&.active {
  color: $accent-pink; background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
  box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
}
}

/* ================= Tab 1: 列表区 ================= */
.filters-row { display: flex; flex-wrap: wrap; gap: 12px; }
.glass-select :deep(.el-input__wrapper) { background: rgba(255,255,255,0.7); backdrop-filter: blur(12px); border-radius: 12px; box-shadow: inset 0 2px 6px rgba(0,0,0,0.02); border: 1px solid rgba(255,255,255,0.9); }
.glass-select :deep(.el-input__inner) { color: $text-main; font-weight: 600;}

.list-toolbar { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; margin-bottom: 12px; gap: 12px; }
.sort-pills { display: flex; gap: 4px; padding: 4px;}
.pill {
padding: 6px 16px; border: none; border-radius: 999px; font-size: 13px; font-weight: 600; color: $text-sub; background: transparent; cursor: pointer; transition: all 0.2s;
&.active { background: #fff; color: $accent-blue; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
}
.btn-refresh-sm { padding: 6px 16px; gap: 6px;}
.invite-policy-hint { text-align: center; color: $text-sub; font-size: 12px; padding: 8px; margin-bottom: 16px;}

.invite-list { min-height: 400px; }
.invite-items { display: flex; flex-direction: column; gap: 16px; }
.invite-card {
padding: 20px; cursor: pointer; position: relative; overflow: hidden;
transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
&:hover { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(0,0,0,0.06); }
&.invite-card-1v1 {
  background: linear-gradient(135deg, rgba(255,255,255,0.6), rgba(167, 139, 250, 0.05));
  border-color: rgba(167, 139, 250, 0.3);
}
}
.card-glow-indicator { position: absolute; left: 0; top: 0; bottom: 0; width: 40px; filter: blur(20px); opacity: 0.15; pointer-events: none; }
.invite-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; position: relative; z-index: 1;}
.invite-header-badges { display: flex; align-items: center; gap: 8px; }
.invite-type-badge { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; color: white; text-shadow: 0 1px 2px rgba(0,0,0,0.2); }
.invite-badge-1v1 { padding: 2px 10px; font-size: 11px; font-weight: 700; color: $accent-pink; background: rgba(255,51,102,0.1); border-radius: 999px; }

.invite-title { font-size: 18px; font-weight: 800; color: $text-main; margin: 0 0 8px; position: relative; z-index: 1;}
.invite-content { font-size: 14px; color: $text-sub; line-height: 1.6; margin: 0 0 12px; white-space: pre-wrap; word-break: break-word; position: relative; z-index: 1;}

.invite-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 16px; padding: 8px; position: relative; z-index: 1;}
.invite-1v1-users { display: flex; align-items: center; gap: 8px; }
.invite-1v1-user { display: flex; align-items: center; gap: 6px; cursor: pointer; &:hover .invite-1v1-name { color: $accent-blue; } }
.avatar-sm { border-radius: 50%; border: 1px solid #fff; }
.invite-1v1-name { font-size: 13px; font-weight: 600; color: $text-sub; }
.invite-1v1-vs { font-size: 12px; color: #cbd5e1; }
.meta-item { display: flex; align-items: center; gap: 4px; font-size: 13px; color: $text-sub; font-weight: 500;}
.meta-item-clickable { cursor: pointer; &:hover { color: $accent-blue; } }

.invite-footer { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; position: relative; z-index: 1; margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,0.05);}
.participants-info { display: flex; align-items: center; gap: 6px; font-size: 13px; color: $text-sub; font-weight: 600;}
.participant-bar { width: 60px; height: 6px; background: rgba(0,0,0,0.05); border-radius: 999px; overflow: hidden; }
.participant-bar-fill { height: 100%; border-radius: 999px; transition: width 0.3s ease; }
.urgent-tag { padding: 4px 12px; background: linear-gradient(135deg, #ff6b6b, #ee5a24); color: white; border-radius: 999px; font-size: 11px; font-weight: 800; }
.rating-info { font-size: 12px; font-weight: 800; color: #e6a23c; border: 1px solid rgba(230, 162, 60, 0.3);}
.invite-pending-actions { margin-left: auto; display: flex; gap: 8px; }

.loading-hint, .sentinel { text-align: center; padding: 40px; color: $text-sub; font-size: 14px; font-weight: 600; display: flex; flex-direction: column; align-items: center; justify-content: center;}

// ==================== 2. 等待雷达 Tab (深度重构) ====================
.wait-list { padding-top: 16px; }
.wait-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.wait-section-title { font-size: 20px; font-weight: 800; color: $text-main; margin: 0; display: flex; align-items: center; gap: 8px; }

.wait-items { display: flex; flex-direction: column; gap: 16px; }
.wait-card { 
padding: 20px; cursor: pointer; transition: all 0.3s;
&:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(0,0,0,0.05); border-color: rgba($accent-pink, 0.3); }
}

.wait-card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.wait-types { display: flex; flex-wrap: wrap; gap: 8px; }
.wait-type-tag { padding: 4px 14px; font-size: 13px; font-weight: 700; color: $accent-blue; background: rgba(79, 140, 255, 0.1); border-color: transparent;}
.wait-status-tags { display: flex; gap: 8px; }
.expired-tag { font-size: 12px; color: $text-sub; background: rgba(0,0,0,0.05); }
.pending-tag { font-size: 12px; font-weight: 700; color: $accent-pink; background: rgba(255,51,102,0.05); border-color: transparent;}
.auto-accept-tag { font-size: 12px; font-weight: 700; color: #10b981; background: rgba(16, 185, 129, 0.1); border-color: transparent;}

.wait-card-middle { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.wait-info-row { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.info-label { color: $text-sub; font-weight: 600; }
.info-value { color: $text-main; font-weight: 700; }

.wait-card-bottom { 
display: flex; justify-content: space-between; align-items: center; 
padding-top: 16px; border-top: 1px solid rgba(0,0,0,0.05); 
}
.expire-time { font-size: 12px; color: $text-sub; display: flex; align-items: center; gap: 4px;}
.btn-cancel-wait { 
background: transparent; border: none; color: #f87171; font-size: 14px; font-weight: 700; cursor: pointer; transition: all 0.2s;
&:hover { color: #ef4444; text-shadow: 0 2px 8px rgba(239,68,68,0.3); }
}

// ==================== 3. 统计历史 Tab ====================
.stats-card { padding: 24px; margin-bottom: 24px; }
.stats-title { font-size: 16px; font-weight: 800; color: $text-main; margin: 0 0 16px; display: flex; align-items: center; gap: 8px; .title-accent { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, $accent-pink, $accent-orange); } }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-item { display: flex; flex-direction: column; align-items: center; text-align: center; }
.stat-value { font-size: 28px; font-weight: 900; margin-bottom: 4px; }
.stat-label { font-size: 12px; color: $text-sub; }

.history-filters { display: flex; justify-content: space-between; align-items: center; padding: 12px; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.history-range, .history-types { display: flex; gap: 8px; }

.history-items { display: flex; flex-direction: column; gap: 12px; }
.history-card { 
padding: 16px; cursor: pointer; transition: all 0.3s;
&:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.05); border-color: rgba($accent-pink, 0.3); }
}
.history-card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.history-title { font-size: 16px; font-weight: 800; color: $text-main; margin: 0 0 12px; }
.history-meta { display: flex; flex-wrap: wrap; gap: 12px; padding: 8px 12px; width: fit-content; }

@for $i from 1 through 8 { .invite-card:nth-child(#{$i}) { animation-delay: #{$i * 0.05}s; } }

@media (max-width: 640px) {
.invite-page { padding: 12px 12px 80px; }
.page-header { top: 8px; padding: 16px; border-radius: 20px;}
.page-title { font-size: 22px; }
.filters-row :deep(.el-select) { width: calc(50% - 6px); }
.stats-grid { grid-template-columns: repeat(2, 1fr); }
.invite-card { padding: 16px; }
.wait-card-top { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>