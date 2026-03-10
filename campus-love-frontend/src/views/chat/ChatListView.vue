<template>
  <div class="chat-list-page">
    <div class="page-header">
      <h2 class="page-title">消息</h2>
    </div>

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
            <span class="conv-name text-ellipsis">{{ conv.nickname }}</span>
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
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useChatStore } from '@/store/chatStore'
import { storeToRefs } from 'pinia'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><rect fill="%23f0f2f5" width="48" height="48" rx="24"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="22">👤</text></svg>'
const chatStore = useChatStore()
const { conversations } = storeToRefs(chatStore)

/** 后端 lastTime 现为 yyyy-MM-dd HH:mm:ss；格式化为 今天/昨天 HH:mm 或 月/日 时:分，避免显示错误 */
function formatConvTime(lastTime: string): string {
  if (!lastTime || !lastTime.trim()) return ''
  const s = lastTime.trim()
  let date: Date | null = null
  // ISO 带 Z 或 +08:00：按 UTC 解析后转本地显示
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$/i.test(s)) {
    date = new Date(s)
  }
  // 完整时间 yyyy-MM-dd HH:mm:ss（按本地时间解析，与后端 DATETIME_FMT 一致）
  if (!date) {
    const fullMatch = s.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?/)
    if (fullMatch) {
      const [, y, M, d, h, m] = fullMatch
      date = new Date(Number(y), Number(M) - 1, Number(d), Number(h), Number(m || 0), 0)
    }
  }
  // 短格式 MM-dd HH:mm（兼容旧数据，按当前年解析）
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

onMounted(() => { chatStore.fetchConversations() })
</script>

<style lang="scss" scoped>
.chat-list-page { padding: 0; }

.page-header {
  padding: 20px 24px;
  position: sticky;
  top: 0;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  z-index: 10;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .page-title { font-size: 20px; font-weight: 700; }
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
  transition: transform $transition-fast, box-shadow $transition-fast;

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
  background: $danger;
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
