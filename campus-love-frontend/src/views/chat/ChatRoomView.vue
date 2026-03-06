<template>
  <div class="chat-room">
    <div class="chat-header">
      <button class="back-btn" @click="$router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <img :src="otherUser?.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" />
      <div class="header-info">
        <div class="header-name">{{ otherUser?.nickname || '加载中...' }}</div>
        <div class="header-status">{{ chatStore.connected ? '在线' : '离线' }}</div>
      </div>
      <button class="more-btn" @click="$router.push(`/profile/${otherUserId}`)">
        <el-icon><User /></el-icon>
      </button>
    </div>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-row', { mine: msg.senderId === myId }]"
      >
        <img
          v-if="msg.senderId !== myId"
          :src="msg.senderAvatar || defaultAvatar"
          class="avatar msg-avatar"
          width="36"
          height="36"
        />
        <div class="message-bubble">
          <!-- 邀约消息：带查看详情按钮 -->
          <template v-if="msg.msgType === 2">
            <p class="msg-content">{{ msg.content }}</p>
            <el-button
              class="invite-link-btn"
              type="primary"
              text
              size="small"
              @click="goToInvite(msg.content)"
            >
              查看邀约详情
            </el-button>
            <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
          </template>
          <!-- 普通文本消息 -->
          <template v-else>
            <p class="msg-content">{{ msg.content }}</p>
            <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
          </template>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <el-input v-model="inputText" placeholder="输入消息..." size="large" @keyup.enter="handleSend">
        <template #append>
          <button class="send-btn" :disabled="!inputText.trim()" @click="handleSend">
            <el-icon :size="20"><Promotion /></el-icon>
          </button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/store/chatStore'
import { useUserStore } from '@/store/userStore'
import { getChatHistory, markAsRead } from '@/api/chatApi'
import { getUserProfile, type UserProfile } from '@/api/userApi'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { formatLocalDateTime } from '@/utils/dateTime'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36"><rect fill="%23f0f2f5" width="36" height="36" rx="18"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="16">👤</text></svg>'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const { currentMessages } = storeToRefs(chatStore)

const otherUserId = computed(() => Number(route.params.userId))
const myId = computed(() => userStore.user?.id)
const otherUser = ref<UserProfile | null>(null)
const inputText = ref('')
const messageListRef = ref<HTMLElement>()

const messages = computed(() => {
  return currentMessages.value.filter(m =>
    (m.senderId === myId.value && m.receiverId === otherUserId.value) ||
    (m.senderId === otherUserId.value && m.receiverId === myId.value)
  )
})

onMounted(async () => {
  chatStore.connectWebSocket()
  try {
    const [profileRes, historyRes] = await Promise.all([
      getUserProfile(otherUserId.value),
      getChatHistory(otherUserId.value, 0, 50),
    ])
    otherUser.value = profileRes.data.data
    const loaded = historyRes.data.data?.reverse() || []
    const myIdNum = myId.value
    const otherId = otherUserId.value
    const belong = (m: { senderId: number; receiverId: number }) =>
      (m.senderId === myIdNum && m.receiverId === otherId) ||
      (m.senderId === otherId && m.receiverId === myIdNum)
    chatStore.setMessagesMergedWithHistory(
      loaded,
      m => belong(m),
      chatStore.getPendingForUser(otherId)
    )
    markAsRead(otherUserId.value)
    scrollToBottom()
  } catch { /* handled */ }
})

watch(messages, () => { nextTick(scrollToBottom) }, { deep: true })

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  const now = formatLocalDateTime()
  chatStore.pushOptimisticMessage(
    {
      id: -Date.now(),
      senderId: myId.value!,
      receiverId: otherUserId.value,
      senderNickname: userStore.user?.nickname ?? '',
      senderAvatar: userStore.user?.avatarUrl ?? null,
      content: text,
      msgType: 1,
      isRead: false,
      createdAt: now,
    },
    { pendingOtherUserId: otherUserId.value }
  )
  chatStore.sendMessage(otherUserId.value, text)
  inputText.value = ''
  nextTick(scrollToBottom)
}

function goToInvite(content: string) {
  const match = content.match(/INVITE#(\d+)/)
  if (match && match[1]) {
    const inviteId = match[1]
    router.push(`/invite/${inviteId}`)
  } else {
    ElMessage.warning('未找到邀约信息')
  }
}
</script>

<style lang="scss" scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid $border-light;
  background: $bg-primary;
  flex-shrink: 0;

  .back-btn, .more-btn {
    width: 36px; height: 36px;
    display: flex; align-items: center; justify-content: center;
    border-radius: $radius-full;
    transition: background $transition-fast;
    &:hover { background: $bg-tertiary; }
  }

  .header-info { flex: 1; }
  .header-name { font-size: 15px; font-weight: 600; }
  .header-status { font-size: 12px; color: $success; }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 75%;

  &.mine {
    align-self: flex-end;
    flex-direction: row-reverse;

    .message-bubble {
      background: $primary;
      color: white;

      .msg-time { color: rgba(white, 0.7); }
    }
  }
}

.msg-avatar { flex-shrink: 0; }

.message-bubble {
  padding: 10px 14px;
  border-radius: $radius-lg;
  background: $bg-tertiary;
  max-width: 100%;

  .msg-content { font-size: 14px; line-height: 1.5; word-wrap: break-word; }
  .msg-time { font-size: 11px; color: $text-muted; display: block; text-align: right; margin-top: 4px; }
}

.invite-link-btn {
  margin-top: 4px;
  padding: 0;
  font-size: 12px;
}

.chat-input-area {
  padding: 12px 20px;
  border-top: 1px solid $border-light;
  background: $bg-primary;
  flex-shrink: 0;

  :deep(.el-input__wrapper) { border-radius: $radius-full; }
  :deep(.el-input-group__append) { padding: 0; background: none; border: none; box-shadow: none; }

  .send-btn {
    width: 44px; height: 44px;
    display: flex; align-items: center; justify-content: center;
    background: $primary-gradient;
    color: white;
    border-radius: $radius-full;
    transition: opacity $transition-fast;
    cursor: pointer;

    &:disabled { opacity: 0.4; cursor: not-allowed; }
  }
}
</style>
