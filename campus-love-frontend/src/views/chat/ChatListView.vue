<template>
  <div class="message-page">
    <div class="global-aurora-bg"></div>

    <div class="page-shell">
      <div class="page-header">
        <h2 class="page-title text-gradient-warm">消息中心</h2>
      </div>

      <div class="notify-tabs tuner-capsule glass-panel">
        <button
          v-for="tab in notifyTabs"
          :key="tab.key"
          :class="['tuner-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          <div class="tab-content-wrap">
            <el-icon class="tab-icon" :size="18"><component :is="tab.icon" /></el-icon>
            <span class="tab-label">{{ tab.label }}</span>
            <span v-if="tab.count > 0" class="tab-badge pulse-shadow">{{ tab.count > 99 ? '99+' : tab.count }}</span>
          </div>
        </button>
      </div>

      <div v-if="activeTab === 'chat'" class="notify-list panel-entrance">
        <div v-if="displayedConversations.length" class="notify-list-inner">
          <div class="notify-list-header">
            <span class="notify-count">共 {{ displayedConversations.length }} 个星轨连接</span>
            <button v-if="displayedConversations.length" class="glass-btn clear-all-btn" @click="clearTab('chat')">清空</button>
          </div>
          <div class="conversation-list">
            <div
              v-for="conv in displayedConversations"
              :key="conv.userId"
              class="conversation-item glass-card-light"
              @click="$router.push(`/chat/${conv.userId}`)"
            >
              <div v-if="conv.unreadCount > 0" class="card-glow-indicator bg-accent-pink"></div>

              <div class="conv-avatar-wrap">
                <AppAvatar :src="conv.avatarUrl" :name="conv.nickname" :size="52" class="avatar conv-avatar-clickable" @click.stop="$router.push(`/profile/${conv.userId}`)" />
                <span v-if="conv.unreadCount" class="unread-dot pulse-shadow">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
              </div>
              <div class="conv-content">
                <div class="conv-top">
                  <span class="conv-name text-main">{{ followStore.getDisplayName(conv.userId, conv.nickname) }}</span>
                  <span class="conv-time text-sub">{{ formatConvTime(conv.lastTime) }}</span>
                </div>
                <div v-if="conv.msgType === 5" class="conv-msg-share-card glass-pill-light" @click.stop="() => { const id = getPostIdFromShare(conv.lastMessage); if (id != null) $router.push(`/feed/${id}`) }">
                  <el-icon class="share-icon text-accent-pink" :size="16"><Share /></el-icon>
                  <span class="share-text text-sub">{{ getShareContent(conv.lastMessage) }}</span>
                </div>
                <p v-else class="conv-msg text-sub text-ellipsis" :class="{'text-main font-bold': conv.unreadCount > 0}">
                  {{ displayLastMessage(conv.lastMessage, conv.msgType) }}
                </p>
              </div>
              <button class="notify-delete-btn" title="从列表移除" @click.stop.prevent="dismissConversation(conv.userId)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <AppEmptyState v-else text="暂无消息记录" hint="在引力场中关注感兴趣的人，开始共鸣吧">
          <template #icon><el-icon class="empty-icon text-accent-blue" :size="48"><ChatDotRound /></el-icon></template>
        </AppEmptyState>
      </div>

      <div v-if="activeTab === 'likes'" class="notify-list panel-entrance">
        <div v-if="displayedLikes.length" class="notify-list-inner">
          <div class="notify-list-header">
            <span class="notify-count">共收获 {{ displayedLikes.length }} 次心动共鸣</span>
            <button v-if="displayedLikes.length" class="glass-btn clear-all-btn" @click="clearTab('likes')">清空</button>
          </div>
          <div class="conversation-list">
            <div
              v-for="item in displayedLikes"
              :key="item.id"
              class="notify-item glass-card-light"
              :class="{ 'opacity-60': readIds.has(item.id) }"
              @click="handleNotifyClick(item, 'post')"
            >
              <div v-if="!readIds.has(item.id)" class="card-glow-indicator bg-accent-pink"></div>
              
              <AppAvatar :src="item.senderAvatarUrl" :name="item.senderNickname" :size="44" class="avatar" @click.stop="goToProfile(item.senderId)" />
              <div class="notify-body">
                <div class="notify-text-block">
                  <span class="notify-sender text-main font-bold" @click.stop="goToProfile(item.senderId)">{{ item.senderNickname }}</span>
                  <span class="notify-text text-sub">{{ item.type === 'COMMENT_LIKE' ? '赞了你的评论' : '赞了你的动态' }}</span>
                </div>
                <div class="notify-time text-sub">{{ formatConvTime(item.createdAt) }}</div>
              </div>
              <span v-if="!readIds.has(item.id)" class="unread-indicator pulse-dot" />
              <button class="notify-delete-btn" title="从列表移除" @click.stop.prevent="dismissLike(item.id)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <AppEmptyState v-else text="暂无点赞" hint="多发射一些引力波，吸引同频的 TA 吧">
          <template #icon><el-icon class="empty-icon text-accent-pink" :size="48"><StarFilled /></el-icon></template>
        </AppEmptyState>
      </div>

      <div v-if="activeTab === 'followers'" class="notify-list panel-entrance">
        <div v-if="displayedFollowers.length" class="notify-list-inner">
          <div class="notify-list-header">
            <span class="notify-count">共 {{ displayedFollowers.length }} 位新同频者</span>
            <button v-if="displayedFollowers.length" class="glass-btn clear-all-btn" @click="clearTab('followers')">清空</button>
          </div>
          <div class="conversation-list">
            <div
              v-for="item in displayedFollowers"
              :key="item.id"
              class="notify-item glass-card-light"
              :class="{ 'opacity-60': readIds.has(item.id) }"
              @click="handleNotifyClick(item, 'profile')"
            >
              <div v-if="!readIds.has(item.id)" class="card-glow-indicator bg-accent-blue"></div>
              
              <AppAvatar :src="item.senderAvatarUrl" :name="item.senderNickname" :size="44" class="avatar" @click.stop="goToProfile(item.senderId)" />
              <div class="notify-body">
                <div class="notify-text-block">
                  <span class="notify-sender text-main font-bold">{{ item.senderNickname }}</span>
                  <span class="notify-text text-sub">关注了你</span>
                </div>
                <div class="notify-time text-sub">{{ formatConvTime(item.createdAt) }}</div>
              </div>
              <button
                :class="['action-btn-sm', followStore.isFollowed(item.senderId) ? 'glass-btn' : 'glow-btn-warm']"
                @click.stop="handleFollowBack(item.senderId)"
              >
                {{ followStore.isFollowed(item.senderId) ? '已关注' : '回关' }}
              </button>
              <button class="notify-delete-btn ml-2" title="标记已读" @click.stop.prevent="dismissFollower(item.senderId)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <AppEmptyState v-else text="暂无新增关注">
          <template #icon><el-icon class="empty-icon text-accent-orange" :size="48"><UserFilled /></el-icon></template>
        </AppEmptyState>
      </div>

      <div v-if="activeTab === 'invites'" class="notify-list panel-entrance">
        <div v-if="displayedInvites.length" class="notify-list-inner">
          <div class="notify-list-header">
            <span class="notify-count">共 {{ displayedInvites.length }} 条邀约动态</span>
            <button v-if="displayedInvites.length" class="glass-btn clear-all-btn" @click="clearTab('invites')">清空</button>
          </div>
          <div class="conversation-list">
            <div
              v-for="item in displayedInvites"
              :key="item.id"
              class="notify-item glass-card-light"
              :class="{ 'opacity-60': item.isRead }"
              @click="handleInviteNotifyClick(item)"
            >
              <div v-if="!item.isRead" class="card-glow-indicator bg-accent-orange"></div>
              
              <div class="invite-notify-icon glass-pill-light">
                <el-icon :size="24" class="text-accent-orange"><Calendar /></el-icon>
              </div>
              <div class="notify-body">
                <span class="notify-sender text-main font-bold block mb-1">{{ item.title }}</span>
                <div class="notify-content-text text-sub">{{ item.content }}</div>
                <div class="notify-time text-sub mt-2">{{ formatConvTime(item.createdAt) }}</div>
              </div>
              <span v-if="!item.isRead" class="unread-indicator pulse-dot" />
              <button class="notify-delete-btn" title="从列表移除" @click.stop.prevent="dismissInviteNotify(item.id)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <AppEmptyState v-else text="暂无邀约通知" hint="加入或发起邀约后，相关动态会在这里提醒你">
          <template #icon><el-icon class="empty-icon text-accent-orange" :size="48"><Calendar /></el-icon></template>
        </AppEmptyState>
      </div>

      <div v-if="activeTab === 'comments'" class="notify-list panel-entrance">
        <div v-if="displayedComments.length" class="notify-list-inner">
          <div class="notify-list-header">
            <span class="notify-count">共 {{ displayedComments.length }} 条回应</span>
            <button v-if="displayedComments.length" class="glass-btn clear-all-btn" @click="clearTab('comments')">清空</button>
          </div>
          <div class="conversation-list">
            <div
              v-for="item in displayedComments"
              :key="item.id"
              class="notify-item glass-card-light"
              :class="{ 'opacity-60': readIds.has(item.id) }"
              @click="handleNotifyClick(item, 'post')"
            >
              <div v-if="!readIds.has(item.id)" class="card-glow-indicator bg-accent-blue"></div>
              
              <AppAvatar :src="item.senderAvatarUrl" :name="item.senderNickname" :size="44" class="avatar" @click.stop="goToProfile(item.senderId)" />
              <div class="notify-body">
                <span class="notify-sender text-main font-bold" @click.stop="goToProfile(item.senderId)">{{ item.senderNickname }}</span>
                <div class="notify-text text-sub mt-1">{{ item.content }}</div>
                <div class="notify-time text-sub mt-2">{{ formatConvTime(item.createdAt) }}</div>
              </div>
              <span v-if="!readIds.has(item.id)" class="unread-indicator pulse-dot" />
              <button class="notify-delete-btn" title="从列表移除" @click.stop.prevent="dismissComment(item.id)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
        <AppEmptyState v-else text="暂无评论和@">
          <template #icon><el-icon class="empty-icon text-accent-blue" :size="48"><ChatDotRound /></el-icon></template>
        </AppEmptyState>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/store/chatStore'
import { useFollowStore } from '@/store/followStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useNotifyDismissStore } from '@/store/notifyDismissStore'
import { followUser, getFollowerList, type FollowUser } from '@/api/followApi'
import { ElMessage } from 'element-plus'
import { Delete, Calendar, Share, StarFilled, UserFilled, ChatDotRound } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import request from '@/api/request'
import type { ApiResult } from '@/api/request'
import AppEmptyState from '@/components/AppEmptyState.vue'
import AppAvatar from '@/components/AppAvatar.vue'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const followStore = useFollowStore()
const badgeStore = useBadgeStore()
const notifyDismissStore = useNotifyDismissStore()
const { conversations } = storeToRefs(chatStore)

const activeTab = ref('chat')

const FOLLOWER_NOTIFY_ID_BASE = 2_000_000_000
const NOTIFY_READ_KEY = 'campus_love_notify_read_ids'

function loadReadIds(): Set<number> {
  try {
    const stored = localStorage.getItem(NOTIFY_READ_KEY)
    if (stored) return new Set(JSON.parse(stored) as number[])
  } catch { /* ignore */ }
  return new Set()
}

function saveReadIds() {
  try {
    localStorage.setItem(NOTIFY_READ_KEY, JSON.stringify([...readIds.value]))
  } catch { /* ignore */ }
}

const readIds = ref<Set<number>>(loadReadIds())

interface SocialNotification {
  id: number; senderId: number; senderNickname: string; senderAvatarUrl: string | null;
  type: string; content: string; postId?: number; createdAt: string
}

interface InviteNotification {
  id: number; inviteId: number | null; relatedId?: number | null;
  type: string; title: string; content: string; isRead: boolean; createdAt: string
}

const socialNotifications = ref<SocialNotification[]>([])
const followerUsers = ref<FollowUser[]>([])
const inviteNotifications = ref<InviteNotification[]>([])

const INVITE_DISMISSED_KEY = 'campus_love_invite_notify_dismissed'
function loadDismissedInviteIds(): Set<number> {
  try {
    const s = localStorage.getItem(INVITE_DISMISSED_KEY)
    if (s) return new Set(JSON.parse(s) as number[])
  } catch { /* ignore */ }
  return new Set()
}
function saveDismissedInviteIds() {
  try { localStorage.setItem(INVITE_DISMISSED_KEY, JSON.stringify([...dismissedInviteIds.value])) } catch { /* ignore */ }
}
const dismissedInviteIds = ref<Set<number>>(loadDismissedInviteIds())

const likeNotifications = computed(() => socialNotifications.value.filter(n => n.type === 'LIKE' || n.type === 'COMMENT_LIKE'))
const followerNotifications = computed(() => followerUsers.value.map((u) => ({
  id: FOLLOWER_NOTIFY_ID_BASE + u.userId, senderId: u.userId, senderNickname: u.nickname, senderAvatarUrl: u.avatarUrl,
  type: 'NEW_FOLLOWER', content: '关注了你', createdAt: ''
} as SocialNotification)))
const commentNotifications = computed(() => socialNotifications.value.filter(n => n.type === 'COMMENT' || n.type === 'MENTION'))

const displayedConversations = computed(() => conversations.value.filter(c => !notifyDismissStore.dismissedChatIds.has(c.userId)))
const displayedLikes = computed(() => likeNotifications.value.filter(n => !notifyDismissStore.raw.likes.includes(n.id)))
const displayedFollowers = computed(() => followerNotifications.value)
const displayedComments = computed(() => commentNotifications.value.filter(n => !notifyDismissStore.raw.comments.includes(n.id)))
const displayedInvites = computed(() => inviteNotifications.value.filter(n => !dismissedInviteIds.value.has(n.id)))

function dismissLike(id: number) { if (notifyDismissStore.dismissLike(id)) ElMessage.success('已移除') }
function dismissFollower(senderId: number) { readIds.value.add(FOLLOWER_NOTIFY_ID_BASE + senderId); saveReadIds(); ElMessage.success('已标记已读') }
function dismissComment(id: number) { if (notifyDismissStore.dismissComment(id)) ElMessage.success('已移除') }
function dismissConversation(userId: number) { if (notifyDismissStore.dismissChat(userId)) ElMessage.success('已移除') }
function dismissInviteNotify(id: number) { dismissedInviteIds.value.add(id); saveDismissedInviteIds(); ElMessage.success('已移除') }

function clearTab(tab: 'chat' | 'likes' | 'followers' | 'comments' | 'invites') {
  if (tab === 'chat') notifyDismissStore.clearChats(conversations.value.map(c => c.userId))
  else if (tab === 'likes') notifyDismissStore.clearLikes(likeNotifications.value.map(n => n.id))
  else if (tab === 'followers') { followerNotifications.value.forEach(n => readIds.value.add(n.id)); saveReadIds(); void badgeStore.markFollowersViewed() }
  else if (tab === 'invites') { inviteNotifications.value.forEach(n => dismissedInviteIds.value.add(n.id)); saveDismissedInviteIds() }
  else notifyDismissStore.clearComments(commentNotifications.value.map(n => n.id))
  ElMessage.success('已清空')
}

const unreadLikeCount = computed(() => displayedLikes.value.filter(n => !readIds.value.has(n.id)).length)
const unreadFollowerCount = computed(() => followerNotifications.value.filter(n => !readIds.value.has(n.id)).length)
const unreadCommentCount = computed(() => displayedComments.value.filter(n => !readIds.value.has(n.id)).length)
const unreadInviteCount = computed(() => displayedInvites.value.filter(n => !n.isRead).length)

const notifyTabs = computed(() => [
  { key: 'chat', label: '私信', icon: 'ChatDotRound', count: displayedConversations.value.reduce((s, c) => s + (c.unreadCount || 0), 0) },
  { key: 'invites', label: '邀约', icon: 'Calendar', count: unreadInviteCount.value },
  { key: 'likes', label: '赞', icon: 'StarFilled', count: unreadLikeCount.value },
  { key: 'followers', label: '关注', icon: 'UserFilled', count: Math.max(badgeStore.badges.newFollowerCount || 0, unreadFollowerCount.value) },
  { key: 'comments', label: '评论', icon: 'ChatDotRound', count: unreadCommentCount.value },
])

watch(activeTab, (tab) => {
  if (tab === 'likes') { likeNotifications.value.forEach(n => readIds.value.add(n.id)); badgeStore.markFeedActivityViewed(); saveReadIds() }
  else if (tab === 'followers') { followerNotifications.value.forEach(n => readIds.value.add(n.id)); badgeStore.markFollowersViewed(); saveReadIds() }
  else if (tab === 'comments') { commentNotifications.value.forEach(n => readIds.value.add(n.id)); badgeStore.markFeedActivityViewed(); saveReadIds() }
  else if (tab === 'invites') { markAllInviteNotificationsRead(); badgeStore.markInviteActivityViewed() }
})

async function loadSocialNotifications() {
  try { socialNotifications.value = (await request.get<ApiResult<SocialNotification[]>>('/feed/social-notifications')).data.data || [] }
  catch { socialNotifications.value = [] }
}

async function loadInviteNotifications() {
  try {
    const res = await request.get<ApiResult<any[]>>('/notification?unreadOnly=false')
    inviteNotifications.value = (res.data.data || [])
      .filter((n) => (n.type || '').startsWith('INVITE_') || (n.type || '').startsWith('PAIR_DATE_'))
      .map((n) => ({ ...n, createdAt: n.createdAt ? String(n.createdAt) : '' }))
  } catch { inviteNotifications.value = [] }
}

async function markAllInviteNotificationsRead() {
  const unread = displayedInvites.value.filter(n => !n.isRead)
  for (const n of unread) { try { await request.post(`/notification/${n.id}/read`); n.isRead = true } catch {} }
}

async function handleInviteNotifyClick(item: InviteNotification) {
  if (!item.isRead) { try { await request.post(`/notification/${item.id}/read`); item.isRead = true } catch {} }
  const t = item.type || ''
  if (t.startsWith('PAIR_DATE_')) {
    const rid = item.relatedId != null ? Number(item.relatedId) : NaN
    if (Number.isFinite(rid) && rid > 0) { router.push(`/moment/pair-date/${rid}`); return }
    router.push('/moment/result'); return
  }
  if (item.inviteId) router.push({ path: `/invite/${item.inviteId}`, query: { from: 'notify' } })
}

async function handleFollowBack(userId: number) {
  if (followStore.isFollowed(userId)) return
  try { await followUser(userId); followStore.addFollowed(userId); ElMessage.success('关注成功') }
  catch { ElMessage.error('关注失败') }
}

function goToProfile(userId: number) { router.push(`/profile/${userId}`) }

function handleNotifyClick(item: SocialNotification, target: 'post' | 'profile') {
  readIds.value.add(item.id); saveReadIds()
  if (target === 'post' && item.postId != null) {
    const n = Number(item.postId)
    if (Number.isFinite(n) && n > 0) { router.push(`/feed/${n}`); return }
  }
  router.push(`/profile/${item.senderId}`)
}

function formatConvTime(lastTime: string): string {
  if (!lastTime || !lastTime.trim()) return ''
  const s = lastTime.trim()
  let date: Date | null = null
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$/i.test(s)) date = new Date(s)
  if (!date) {
    const fullMatch = s.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?/)
    if (fullMatch) date = new Date(Number(fullMatch[1]), Number(fullMatch[2]) - 1, Number(fullMatch[3]), Number(fullMatch[4]), Number(fullMatch[5] || 0), 0)
  }
  if (!date) {
    const shortMatch = s.match(/^(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})/)
    if (shortMatch) date = new Date(new Date().getFullYear(), Number(shortMatch[1]) - 1, Number(shortMatch[2]), Number(shortMatch[3]), Number(shortMatch[4]), 0)
  }
  if (date && !Number.isNaN(date.getTime())) return formatByDate(date)
  return s
}

function formatByDate(date: Date): string {
  const now = new Date(); const diffMs = now.getTime() - date.getTime(); const diffMin = Math.floor(diffMs / 60000); const diffHour = Math.floor(diffMs / 3600000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()); const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  if (d.getTime() === today.getTime()) return diffHour < 6 ? `${diffHour}小时前` : timeStr
  const yesterday = new Date(today); yesterday.setDate(yesterday.getDate() - 1)
  if (d.getTime() === yesterday.getTime()) return `昨天 ${timeStr}`
  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })
}

function displayLastMessage(text: string | undefined, msgType?: number): string {
  if (!text || msgType === 5) return ''
  return text.includes('INVITE#') ? '[收到一个邀约]' : text
}

function getPostIdFromShare(content: string | undefined): number | null {
  if (!content) return null
  try { const n = Number(JSON.parse(content).postId); return (Number.isFinite(n) && n > 0) ? Math.floor(n) : null } catch { return null }
}

function getShareContent(content: string | undefined): string {
  if (!content) return '[帖子分享]'
  try { const d = JSON.parse(content); const c = d.postContent || ''; return `${d.postNickname || ''}：${c.substring(0, 30)}${c.length > 30 ? '...' : ''}` } catch { return '[帖子分享]' }
}

onMounted(() => {
  void badgeStore.fetchBadges()
  chatStore.fetchConversations()
  loadSocialNotifications(); loadFollowers(); loadInviteNotifications()
  if (route.query.tab === 'invites') activeTab.value = 'invites'
  if (followStore.followedIds.length === 0) followStore.loadFollowedIds()
})

async function loadFollowers() {
  try { followerUsers.value = (await getFollowerList()).data.data || [] } catch { followerUsers.value = [] }
}
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 消息页 UI
   ========================================== */
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;

.message-page { 
  min-height: 100vh; position: relative; padding-bottom: 80px; 
}

// 弥散的极光背景底色
.global-aurora-bg {
  position: fixed; inset: 0; pointer-events: none; z-index: -1; background: #f8fafc;
  &::after {
    content: ''; position: absolute; inset: 0;
    background: 
      radial-gradient(circle at 0% 10%, rgba(79, 140, 255, 0.08), transparent 40%),
      radial-gradient(circle at 100% 40%, rgba(255, 51, 102, 0.06), transparent 40%),
      radial-gradient(circle at 50% 100%, rgba(255, 123, 84, 0.05), transparent 50%);
  }
}

.page-shell {
  width: 100%; max-width: 800px; margin: 0 auto; padding: 0 16px; position: relative; z-index: 1;
}

// ================= 通用极光类 =================
.glass-panel { background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(24px); border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.03); border-radius: 20px; }
.glass-card-light { 
  background: rgba(255, 255, 255, 0.85); /* 提高不透明度，增强与背景的对比 */
  backdrop-filter: blur(16px); 
  border: 1px solid #ffffff; 
  border-radius: 20px; 
  box-shadow: 0 8px 24px rgba(31, 38, 135, 0.06); /* 加深阴影 */
}
.glass-pill { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px; }
.glass-pill-light { background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px; }

.text-gradient-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800; }
.text-main { color: $text-main; }
.text-sub { color: $text-sub; }
.text-accent-pink { color: $accent-pink; }
.text-accent-blue { color: $accent-blue; }
.font-bold { font-weight: 700; }
.text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.glow-btn-warm {
  height: 36px; padding: 0 16px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; font-size: 13px; font-weight: 700; cursor: pointer;
  box-shadow: 0 4px 15px rgba(255, 51, 102, 0.3); transition: all 0.3s;
  &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255, 51, 102, 0.4); }
}
.glass-btn {
  height: 36px; padding: 0 16px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-sub; font-size: 13px; font-weight: 700; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; transition: all 0.3s;
  &:hover { background: #fff; color: $text-main; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
}
.action-btn-sm { @extend .glow-btn-warm; height: 32px; padding: 0 14px; font-size: 12px; }

// ================= 动画特效 =================
.panel-entrance { animation: rise-in 0.5s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
@keyframes rise-in { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
.pulse-shadow { animation: pulse-shadow 2s infinite; }
@keyframes pulse-shadow { 0% { box-shadow: 0 0 0 0 rgba(255,51,102, 0.4); } 70% { box-shadow: 0 0 0 6px rgba(255,51,102, 0); } 100% { box-shadow: 0 0 0 0 rgba(255,51,102, 0); } }
.pulse-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: $accent-pink; animation: pulse-shadow 2s infinite; }

// ================= 页面结构 =================
.page-header { padding: 24px 0 16px; }
.page-title { font-size: 28px; margin: 0; font-family: $serif; letter-spacing: 1px; }

// --- 胶囊导航 Tabs ---
.notify-tabs {
  display: flex; padding: 6px; gap: 8px; margin-bottom: 20px; overflow-x: auto;
  flex-wrap: nowrap; /* 强制不换行 */
  &::-webkit-scrollbar { display: none; }
}
.tuner-btn {
  flex: 0 0 auto; /* 保证子元素不会被挤压换行，超出则滚动 */
  padding: 10px 16px; border-radius: 999px; text-align: center; font-size: 13px; font-weight: 600;
  color: $text-sub; transition: all 0.3s; border: none; background: transparent; cursor: pointer;
  white-space: nowrap; /* 解决文字分段问题 */
  
  .tab-content-wrap { display: flex; align-items: center; justify-content: center; gap: 6px; position: relative; white-space: nowrap;}
  
  &.active {
    color: $accent-pink; background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
    box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  }
}
.tab-badge {
  background: $accent-pink; color: white; font-size: 10px; font-weight: 800; border-radius: 999px;
  padding: 0 5px; height: 16px; display: inline-flex; align-items: center; justify-content: center;
}

// --- 列表区 ---
.notify-list { min-height: 400px; }
.notify-list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding: 0 8px; }
.notify-count { font-size: 13px; color: $text-sub; font-weight: 600; }
.clear-all-btn { height: 30px; padding: 0 12px; font-size: 12px; }

.conversation-list { display: flex; flex-direction: column; gap: 12px; }

// --- 通用卡片样式 (聊天 & 通知) ---
.conversation-item, .notify-item {
  display: flex; align-items: center; gap: 16px; padding: 16px; cursor: pointer; position: relative; overflow: hidden;
  transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  &:hover { transform: translateY(-2px); box-shadow: 0 12px 30px rgba(0,0,0,0.08); background: rgba(255, 255, 255, 0.95);}
}
.opacity-60 { opacity: 0.6; } // 已读状态变淡

// 侧边发光指示器
.card-glow-indicator { position: absolute; left: -10px; top: 0; bottom: 0; width: 30px; filter: blur(15px); opacity: 0.2; pointer-events: none; }
.bg-accent-pink { background: $accent-pink; }
.bg-accent-blue { background: $accent-blue; }
.bg-accent-orange { background: $accent-orange; }

.conv-avatar-wrap { position: relative; flex-shrink: 0; }
.avatar { border-radius: 50%; object-fit: cover; border: 2px solid #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.05);}
.conv-avatar-clickable { cursor: pointer; transition: transform 0.2s; &:hover{ transform: scale(1.05); } }
.unread-dot {
  position: absolute; top: -2px; right: -2px; background: $accent-pink; color: white; font-size: 10px; font-weight: 800;
  min-width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; border-radius: 999px; border: 2px solid #fff;
}

.conv-content, .notify-body { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center;}
.conv-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.conv-name { font-size: 16px; font-weight: 700; }
.conv-time, .notify-time { font-size: 12px; }

.conv-msg { font-size: 14px; margin: 0; line-height: 1.5; }
.notify-text-block { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 4px; }
.notify-sender { font-size: 15px; cursor: pointer; transition: color 0.2s; &:hover { color: $accent-pink; } }
.notify-text { font-size: 14px; }
.unread-indicator { margin-left: 8px; }

// 分享卡片
.conv-msg-share-card {
  display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer; transition: all 0.2s; width: fit-content; max-width: 100%;
  &:hover { background: rgba(255,255,255,0.8); }
}

// 邀约特殊 Icon
.invite-notify-icon {
  width: 44px; height: 44px; border-radius: 50%; background: rgba(255, 123, 84, 0.1); color: $accent-orange;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; border: 1px solid rgba(255, 123, 84, 0.2);
}

// 删除按钮
.notify-delete-btn {
  flex-shrink: 0; padding: 8px; border: none; background: transparent; color: rgba(0,0,0,0.15); cursor: pointer;
  border-radius: 50%; transition: all 0.2s; opacity: 0; // 默认隐藏，hover显示
  &:hover { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
}
.conversation-item:hover .notify-delete-btn, .notify-item:hover .notify-delete-btn { opacity: 1; }

// 移动端适配
@media (max-width: 640px) {
  .page-header { padding: 16px 0; }
  .notify-tabs { padding: 4px; border-radius: 16px; margin-bottom: 16px;}
  .tuner-btn { padding: 8px 12px; font-size: 12px; }
  .notify-delete-btn { opacity: 1; color: rgba(0,0,0,0.25); } // 移动端始终显示垃圾桶
  .conversation-item, .notify-item { padding: 14px 12px; }
  .avatar { width: 44px !important; height: 44px !important; }
}
</style>