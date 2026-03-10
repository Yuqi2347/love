<template>
  <div class="message-page">
    <div class="page-header">
      <h2 class="page-title">消息</h2>
    </div>

    <!-- 通知类型 Tabs -->
    <div class="notify-tabs">
      <button
        v-for="tab in notifyTabs"
        :key="tab.key"
        :class="['notify-tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
        <span v-if="tab.count > 0" class="tab-badge">{{ tab.count > 99 ? '99+' : tab.count }}</span>
      </button>
    </div>

    <!-- 聊天消息列表（默认） -->
    <div v-if="activeTab === 'chat'">
      <div v-if="conversations.length" class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.userId"
          class="conversation-item"
          @click="$router.push(`/chat/${conv.userId}`)"
        >
          <div class="conv-avatar-wrap">
            <img :src="conv.avatarUrl || defaultAvatar" class="avatar" width="48" height="48" alt="" />
            <span v-if="conv.unreadCount" class="unread-dot">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
          </div>
          <div class="conv-content">
            <div class="conv-top">
              <span class="conv-name text-ellipsis">{{ followStore.getDisplayName(conv.userId, conv.nickname) }}</span>
              <span class="conv-time">{{ formatConvTime(conv.lastTime) }}</span>
            </div>
            <p class="conv-msg text-ellipsis">{{ displayLastMessage(conv.lastMessage) }}</p>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无消息</p>
        <p class="empty-hint">关注感兴趣的人，开始聊天吧</p>
      </div>
    </div>

    <!-- 赞和收藏 -->
    <div v-if="activeTab === 'likes'" class="notify-list">
      <div v-if="likeNotifications.length">
        <div v-for="item in likeNotifications" :key="item.id"
          :class="['notify-item', { 'is-read': readIds.has(item.id) }]"
          @click="handleNotifyClick(item, 'post')"
        >
          <img :src="item.senderAvatarUrl || defaultAvatar" class="avatar" width="40" height="40" @click.stop="goToProfile(item.senderId)" />
          <div class="notify-body">
            <span class="notify-sender" @click.stop="goToProfile(item.senderId)">{{ item.senderNickname }}</span>
            <span class="notify-text">赞了你的动态</span>
            <div class="notify-time">{{ formatConvTime(item.createdAt) }}</div>
          </div>
          <span v-if="!readIds.has(item.id)" class="unread-indicator" />
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">❤️</div>
        <p>暂无赞和收藏</p>
      </div>
    </div>

    <!-- 新增关注 -->
    <div v-if="activeTab === 'followers'" class="notify-list">
      <div v-if="followerNotifications.length">
        <div v-for="item in followerNotifications" :key="item.id"
          :class="['notify-item', { 'is-read': readIds.has(item.id) }]"
          @click="handleNotifyClick(item, 'profile')"
        >
          <img :src="item.senderAvatarUrl || defaultAvatar" class="avatar" width="40" height="40" />
          <div class="notify-body">
            <span class="notify-sender">{{ item.senderNickname }}</span>
            <span class="notify-text">关注了你</span>
            <div class="notify-time">{{ formatConvTime(item.createdAt) }}</div>
          </div>
          <button
            :class="['btn-sm', followStore.isFollowed(item.senderId) ? 'btn-followed' : 'btn-follow']"
            @click.stop="handleFollowBack(item.senderId)"
          >
            {{ followStore.isFollowed(item.senderId) ? '已关注' : '回关' }}
          </button>
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">👥</div>
        <p>暂无新增关注</p>
      </div>
    </div>

    <!-- 评论和@ -->
    <div v-if="activeTab === 'comments'" class="notify-list">
      <div v-if="commentNotifications.length">
        <div v-for="item in commentNotifications" :key="item.id"
          :class="['notify-item', { 'is-read': readIds.has(item.id) }]"
          @click="handleNotifyClick(item, 'post')"
        >
          <img :src="item.senderAvatarUrl || defaultAvatar" class="avatar" width="40" height="40" @click.stop="goToProfile(item.senderId)" />
          <div class="notify-body">
            <span class="notify-sender" @click.stop="goToProfile(item.senderId)">{{ item.senderNickname }}</span>
            <span class="notify-text">{{ item.content }}</span>
            <div class="notify-time">{{ formatConvTime(item.createdAt) }}</div>
          </div>
          <span v-if="!readIds.has(item.id)" class="unread-indicator" />
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">💬</div>
        <p>暂无评论和@</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/store/chatStore'
import { useFollowStore } from '@/store/followStore'
import { useBadgeStore } from '@/store/badgeStore'
import { followUser, getFollowerList, type FollowUser } from '@/api/followApi'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import request from '@/api/request'
import type { ApiResult } from '@/api/request'

const router = useRouter()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><rect fill="%23f0f2f5" width="48" height="48" rx="24"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="22">👤</text></svg>'
const chatStore = useChatStore()
const followStore = useFollowStore()
const badgeStore = useBadgeStore()
const { conversations } = storeToRefs(chatStore)

const activeTab = ref('chat')

// 已读通知 ID 追踪（持久化到 localStorage，刷新/重进不丢失）
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
  id: number
  senderId: number
  senderNickname: string
  senderAvatarUrl: string | null
  type: string // LIKE, NEW_FOLLOWER, COMMENT, MENTION
  content: string
  postId?: number
  createdAt: string
}

const socialNotifications = ref<SocialNotification[]>([])
const followerUsers = ref<FollowUser[]>([])

const likeNotifications = computed(() => socialNotifications.value.filter(n => n.type === 'LIKE'))
const followerNotifications = computed(() => {
  return followerUsers.value.map((u, i) => ({
    id: -i - 1,
    senderId: u.userId,
    senderNickname: u.nickname,
    senderAvatarUrl: u.avatarUrl,
    type: 'NEW_FOLLOWER',
    content: '关注了你',
    createdAt: '',
  } as SocialNotification))
})
const commentNotifications = computed(() => socialNotifications.value.filter(n => n.type === 'COMMENT' || n.type === 'MENTION'))

// 未读计数：总数 - 已读数
const unreadLikeCount = computed(() =>
  likeNotifications.value.filter(n => !readIds.value.has(n.id)).length
)
const unreadFollowerCount = computed(() =>
  followerNotifications.value.filter(n => !readIds.value.has(n.id)).length
)
const unreadCommentCount = computed(() =>
  commentNotifications.value.filter(n => !readIds.value.has(n.id)).length
)

const notifyTabs = computed(() => {
  return [
    { key: 'chat', label: '消息', icon: '💬', count: conversations.value.reduce((s, c) => s + (c.unreadCount || 0), 0) },
    { key: 'likes', label: '赞和收藏', icon: '❤️', count: unreadLikeCount.value },
    { key: 'followers', label: '新增关注', icon: '👥', count: unreadFollowerCount.value },
    { key: 'comments', label: '评论@', icon: '💬', count: unreadCommentCount.value },
  ]
})

// 切换 tab 时自动标记该类别全部已读
watch(activeTab, (tab) => {
  if (tab === 'likes') {
    likeNotifications.value.forEach(n => readIds.value.add(n.id))
    badgeStore.markFeedActivityViewed()
    saveReadIds()
  } else if (tab === 'followers') {
    followerNotifications.value.forEach(n => readIds.value.add(n.id))
    badgeStore.markFollowersViewed()
    saveReadIds()
  } else if (tab === 'comments') {
    commentNotifications.value.forEach(n => readIds.value.add(n.id))
    badgeStore.markFeedActivityViewed()
    saveReadIds()
  }
})

async function loadSocialNotifications() {
  try {
    const res = await request.get<ApiResult<SocialNotification[]>>('/feed/social-notifications')
    socialNotifications.value = res.data.data || []
  } catch {
    // 如果后端接口还未实现，静默处理
    socialNotifications.value = []
  }
}

async function handleFollowBack(userId: number) {
  if (followStore.isFollowed(userId)) return
  try {
    await followUser(userId)
    followStore.addFollowed(userId)
    ElMessage.success('关注成功')
  } catch {
    ElMessage.error('关注失败')
  }
}

function goToProfile(userId: number) {
  router.push(`/profile/${userId}`)
}

function goToPost(postId?: number) {
  if (postId) router.push(`/feed/${postId}`)
}

// 点击单条通知时标记已读 + 跳转
function handleNotifyClick(item: SocialNotification, target: 'post' | 'profile') {
  readIds.value.add(item.id)
  saveReadIds()
  if (target === 'post' && item.postId) {
    router.push(`/feed/${item.postId}`)
  } else {
    router.push(`/profile/${item.senderId}`)
  }
}

function formatConvTime(lastTime: string): string {
  if (!lastTime || !lastTime.trim()) return ''
  const s = lastTime.trim()
  let date: Date | null = null
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$/i.test(s)) {
    date = new Date(s)
  }
  if (!date) {
    const fullMatch = s.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?/)
    if (fullMatch) {
      const [, y, M, d, h, m] = fullMatch
      date = new Date(Number(y), Number(M) - 1, Number(d), Number(h), Number(m || 0), 0)
    }
  }
  if (!date) {
    const shortMatch = s.match(/^(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})/)
    if (shortMatch) {
      const [, M, d, h, m] = shortMatch
      const year = new Date().getFullYear()
      date = new Date(year, Number(M) - 1, Number(d), Number(h), Number(m), 0)
    }
  }
  if (date && !Number.isNaN(date.getTime())) return formatByDate(date)
  return s
}

function formatByDate(date: Date): string {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  if (d.getTime() === today.getTime()) return timeStr
  if (d.getTime() === yesterday.getTime()) return `昨天 ${timeStr}`
  return date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })
}

function displayLastMessage(text: string | undefined): string {
  if (!text) return ''
  return text.includes('INVITE#') ? '[邀约邀请]' : text
}

onMounted(() => {
  chatStore.fetchConversations()
  loadSocialNotifications()
  loadFollowers()
  // 确保关注列表已加载（用于显示备注名）
  if (followStore.followedIds.length === 0) {
    followStore.loadFollowedIds()
  }
})

async function loadFollowers() {
  try {
    const res = await getFollowerList()
    followerUsers.value = res.data.data || []
  } catch {
    followerUsers.value = []
  }
}
</script>

<style lang="scss" scoped>
.message-page { padding: 0; }

.page-header {
  padding: 20px 24px;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.95);
  backdrop-filter: blur(12px);
  z-index: 10;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .page-title { font-size: 20px; font-weight: 700; }
}

.notify-tabs {
  display: flex;
  padding: 12px 16px;
  gap: 8px;
  border-bottom: 1px solid $border-light;
  overflow-x: auto;
}

.notify-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  background: $bg-primary;
  font-size: 13px;
  font-weight: 500;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  position: relative;

  &:hover { border-color: $primary; color: $primary; }

  &.active {
    background: $primary;
    border-color: $primary;
    color: white;
  }

  .tab-icon { font-size: 14px; }
}

.tab-badge {
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  border-radius: $radius-full;
  padding: 0 4px;
}

.notify-tab.active .tab-badge {
  background: white;
  color: $primary;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
  }
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;

  .avatar { width: 48px; height: 48px; border-radius: $radius-full; object-fit: cover; }
}

.unread-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-full;
  padding: 0 4px;
}

.conv-content { flex: 1; min-width: 0; }

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name { font-size: 15px; font-weight: 600; }
.conv-time { font-size: 12px; color: $text-muted; flex-shrink: 0; }
.conv-msg { font-size: 13px; color: $text-secondary; }

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 通知列表
.notify-list {
  padding: 16px;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid $border-light;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: rgba($primary, 0.03); }
  &:last-child { border-bottom: none; }

  &.is-read { opacity: 0.6; }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: $radius-full;
    object-fit: cover;
    flex-shrink: 0;
    cursor: pointer;
  }
}

.unread-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
  align-self: center;
}

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-sender {
  font-weight: 600;
  color: $text-primary;
  margin-right: 4px;
  cursor: pointer;
  &:hover { color: $primary; }
}

.notify-text {
  color: $text-secondary;
  font-size: 14px;
}

.notify-time {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
}

.btn-sm {
  padding: 6px 16px;
  border-radius: $radius-full;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  align-self: center;
}

.btn-follow {
  background: $primary;
  color: white;
  border: 1px solid $primary;
  &:hover { box-shadow: 0 2px 8px rgba($primary, 0.3); }
}

.btn-followed {
  background: $bg-secondary;
  color: $text-secondary;
  border: 1px solid $border-color;
  &:hover { border-color: $text-muted; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  gap: 8px;
  .empty-icon { font-size: 64px; margin-bottom: 8px; }
  p { color: $text-muted; font-size: 15px; }
  .empty-hint { font-size: 13px; }
}
</style>
