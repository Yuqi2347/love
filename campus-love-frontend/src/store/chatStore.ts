import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage, Conversation } from '@/api/chatApi'
import { getConversations } from '@/api/chatApi'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentMessages = ref<ChatMessage[]>([])
  /** 单聊按对方 userId 存待确认消息（乐观消息），用 ref 保证响应式，邀约内嵌聊天等依赖后会正确更新 */
  const pendingByUser = ref<Record<number, ChatMessage[]>>({})
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)

  async function fetchConversations() {
    const res = await getConversations()
    conversations.value = res.data.data || []
  }

  function connectWebSocket() {
    const token = localStorage.getItem('access_token')
    if (!token) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 后端开启了 context-path=/api，WebSocket 实际路径为 /api/ws/chat
    const wsUrl = `${protocol}//${window.location.host}/api/ws/chat?token=${token}`
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => { connected.value = true }

    ws.value.onmessage = (event: MessageEvent) => {
      const msg: ChatMessage = JSON.parse(event.data)
      removeOptimisticIfReplaced(msg)
      removePendingReplaced(msg)
      currentMessages.value.push(msg)
      updateConversation(msg)
    }

    ws.value.onclose = () => {
      connected.value = false
      setTimeout(connectWebSocket, 3000)
    }
  }

  /** 添加乐观消息（发送后立即展示，id 为负数表示临时）；单聊时同时加入 pending，切回会话时可合并 */
  function pushOptimisticMessage(optimistic: ChatMessage, options?: { pendingOtherUserId?: number }) {
    currentMessages.value.push(optimistic)
    if (options?.pendingOtherUserId != null) {
      const id = options.pendingOtherUserId
      const prev = pendingByUser.value[id] ?? []
      pendingByUser.value = { ...pendingByUser.value, [id]: [...prev, optimistic] }
    }
  }

  /** 服务端回显后从 pending 中移除（同 sender+content），避免重复 */
  function removePendingReplaced(msg: ChatMessage) {
    const myId = Number(localStorage.getItem('userId'))
    const otherId = msg.senderId === myId ? msg.receiverId : msg.senderId
    const list = pendingByUser.value[otherId]
    if (!list) return
    const next = list.filter(m => !(m.senderId === msg.senderId && m.content === msg.content))
    pendingByUser.value = { ...pendingByUser.value, [otherId]: next }
  }

  function getPendingForUser(otherUserId: number): ChatMessage[] {
    return pendingByUser.value[otherUserId] ?? []
  }

  /** 移除已由服务端回显替代的乐观消息（同 senderId + content），避免重复 */
  function removeOptimisticIfReplaced(msg: ChatMessage) {
    const tempId = (m: ChatMessage) => typeof m.id === 'number' && m.id < 0
    currentMessages.value = currentMessages.value.filter(
      m => !(tempId(m) && m.senderId === msg.senderId && m.content === msg.content)
    )
  }

  /**
   * 用「接口历史 + 当前会话未在历史中的消息 + 该会话 pending（与历史去重）」合并后覆盖 currentMessages。
   * pending 中与 loaded 同 sender+content 的只保留一条（避免进一次房间消息重复一遍）。
   */
  function setMessagesMergedWithHistory(
    loaded: ChatMessage[],
    belongToThisConversation: (m: ChatMessage) => boolean,
    extra: ChatMessage[] = []
  ) {
    const loadedKeys = new Set(loaded.map(m => `${m.senderId}:${m.content}`))
    const extraDeduped = extra.filter(m => !loadedKeys.has(`${m.senderId}:${m.content}`))
    const loadedIds = new Set(loaded.map(m => m.id))
    const existing = currentMessages.value.filter(belongToThisConversation)
    const toAppend = existing.filter(m => m.id < 0 || !loadedIds.has(m.id))
    const merged = [...loaded, ...toAppend, ...extraDeduped].sort((a, b) =>
      (a.createdAt || '').localeCompare(b.createdAt || '')
    )
    currentMessages.value = merged
  }

  function sendMessage(receiverId: number, content: string, msgType = 1) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ receiverId, content, msgType }))
    }
  }

  function sendGroupMessage(groupId: number, content: string, msgType = 1) {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ groupId, content, msgType }))
    }
  }

  function disconnect() {
    ws.value?.close()
    ws.value = null
    connected.value = false
  }

  function updateConversation(msg: ChatMessage) {
    const otherId = msg.senderId === Number(localStorage.getItem('userId'))
      ? msg.receiverId : msg.senderId
    const idx = conversations.value.findIndex(c => c.userId === otherId)
    if (idx >= 0) {
      const conv = conversations.value[idx]
      if (conv) {
        conv.lastMessage = msg.content
        conv.lastTime = msg.createdAt
      }
    }
  }

  return {
    conversations,
    currentMessages,
    pendingByUser,
    connected,
    fetchConversations,
    connectWebSocket,
    pushOptimisticMessage,
    removeOptimisticIfReplaced,
    removePendingReplaced,
    getPendingForUser,
    setMessagesMergedWithHistory,
    sendMessage,
    sendGroupMessage,
    disconnect,
  }
})
