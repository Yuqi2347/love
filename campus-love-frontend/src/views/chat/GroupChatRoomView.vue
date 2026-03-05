<template>
  <div class="chat-room">
    <div class="chat-header">
      <button class="back-btn" @click="$router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="header-info">
        <div class="header-name">{{ groupName || '群聊' }}</div>
        <div class="header-status">{{ chatStore.connected ? '在线' : '离线' }}</div>
      </div>
    </div>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-row', { mine: msg.senderId === myId }]"
      >
        <div class="message-bubble">
          <p class="msg-sender">{{ msg.senderNickname }}</p>
          <p class="msg-content">{{ msg.content }}</p>
          <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
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
import { useRoute } from 'vue-router'
import { useChatStore } from '@/store/chatStore'
import { useInviteStore } from '@/store/inviteStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const chatStore = useChatStore()
const inviteStore = useInviteStore()
const { currentMessages } = storeToRefs(chatStore)

const groupId = computed(() => Number(route.params.groupId))
const myId = computed(() => Number(localStorage.getItem('userId')))
const inputText = ref('')
const messageListRef = ref<HTMLElement>()

const groupName = computed(() => {
  // 尝试从邀约列表里找到对应的群名称（邀约标题）
  const invite = inviteStore.invites.find(inv => inv.chatGroupId === groupId.value)
  return invite?.title || '群聊'
})

const messages = computed(() => {
  return currentMessages.value.filter(m => m.groupId === groupId.value)
})

onMounted(() => {
  chatStore.connectWebSocket()
})

watch(messages, () => { nextTick(scrollToBottom) }, { deep: true })

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function handleSend() {
  if (!inputText.value.trim()) return
  chatStore.sendGroupMessage(groupId.value, inputText.value.trim())
  inputText.value = ''
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

  .back-btn {
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
  max-width: 80%;

  &.mine {
    align-self: flex-end;
    justify-content: flex-end;
  }
}

.message-bubble {
  padding: 10px 14px;
  border-radius: $radius-lg;
  background: $bg-tertiary;
  max-width: 100%;

  .msg-sender {
    font-size: 12px;
    font-weight: 600;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  .msg-content { font-size: 14px; line-height: 1.5; word-wrap: break-word; }
  .msg-time { font-size: 11px; color: $text-muted; display: block; text-align: right; margin-top: 4px; }
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

