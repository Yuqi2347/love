<template>
  <div class="chat-list-page">
    <div class="page-header">
      <h2 class="page-title">消息</h2>
    </div>

    <div v-if="conversations.length" class="conversation-list">
      <div
v-for="conv in conversations" :key="conv.userId" class="conversation-item"
        @click="$router.push(`/chat/${conv.userId}`)">
        <div class="conv-avatar-wrap">
          <img :src="conv.avatarUrl || defaultAvatar" class="avatar" width="48" height="48" />
          <span v-if="conv.unreadCount" class="unread-dot">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
        </div>
        <div class="conv-content">
          <div class="conv-top">
            <span class="conv-name text-ellipsis">{{ conv.nickname }}</span>
            <span class="conv-time">{{ conv.lastTime }}</span>
          </div>
          <p class="conv-msg text-ellipsis">{{ conv.lastMessage }}</p>
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

onMounted(() => { chatStore.fetchConversations() })
</script>

<style lang="scss" scoped>
.chat-list-page { padding: 0; }

.page-header {
  padding: 20px 24px;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;

  .page-title { font-size: 20px; font-weight: 700; }
}

.conversation-list { display: flex; flex-direction: column; }

.conversation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 24px;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-tertiary; }
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
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
