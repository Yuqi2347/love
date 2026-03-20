import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage, Conversation } from '@/api/chatApi'
import { getConversations } from '@/api/chatApi'
import { useNotifyDismissStore } from '@/store/notifyDismissStore'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentMessages = ref<ChatMessage[]>([])
  /** 单聊按对方 userId 存待确认消息（乐观消息），用 ref 保证响应式，邀约内嵌聊天等依赖后会正确更新 */
  const pendingByUser = ref<Record<number, ChatMessage[]>>({})
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)

  // 心跳与重连控制
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let isReconnecting = false
  let reconnectAttempts = 0
  const HEARTBEAT_INTERVAL = 25000
  const RECONNECT_BASE_DELAY = 5000
  const RECONNECT_MAX_DELAY  = 60000

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (ws.value?.readyState === WebSocket.OPEN) {
        try { ws.value.send(JSON.stringify({ type: 'ping' })) } catch { /* ignore */ }
      }
    }, HEARTBEAT_INTERVAL)
  }

  function stopHeartbeat() {
    if (heartbeatTimer !== null) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function scheduleReconnect() {
    if (isReconnecting) return
    isReconnecting = true
    if (reconnectTimer !== null) clearTimeout(reconnectTimer)
    const delay = Math.min(RECONNECT_BASE_DELAY * Math.pow(1.5, reconnectAttempts), RECONNECT_MAX_DELAY)
    reconnectAttempts++
    reconnectTimer = setTimeout(() => {
      isReconnecting = false
      reconnectTimer = null
      connectWebSocket()
    }, delay)
  }

  async function fetchConversations() {
    const res = await getConversations()
    conversations.value = res.data.data || []
  }

  function connectWebSocket() {
    const token = localStorage.getItem('access_token')
    if (!token) return

    // 若已有一个 OPEN 连接则不重复建立
    if (ws.value && ws.value.readyState === WebSocket.OPEN) return

    stopHeartbeat()

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 后端开启了 context-path=/api，WebSocket 实际路径为 /api/ws/chat
    const wsUrl = `${protocol}//${window.location.host}/api/ws/chat?token=${token}`
    const socket = new WebSocket(wsUrl)
    ws.value = socket

    socket.onopen = () => {
      connected.value = true
      isReconnecting = false
      reconnectAttempts = 0
      startHeartbeat()
    }

    socket.onmessage = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        // 忽略心跳响应
        if (data.type === 'pong' || data.type === 'ping') return
        if (data.type === 'RECALL' && typeof data.messageId === 'number') {
          const mid = data.messageId
          currentMessages.value = currentMessages.value.map(m =>
            m.id === mid ? { ...m, deleted: true, content: '消息已撤回' } : m
          )
          return
        }
        const msg: ChatMessage = data
        removeOptimisticIfReplaced(msg)
        removePendingReplaced(msg)
        // 避免重复添加：若已存在同 id 消息则不再 push（防止 WebSocket 重复推送或竞态）
        if (typeof msg.id === 'number' && msg.id > 0) {
          if (currentMessages.value.some(m => m.id === msg.id)) return
        }
        currentMessages.value.push(msg)
        updateConversation(msg)
      } catch { /* ignore parse errors */ }
    }

    socket.onerror = () => {
      // 错误后 onclose 也会触发，交由 onclose 处理重连
    }

    socket.onclose = () => {
      stopHeartbeat()
      connected.value = false
      // 只有当这次关闭的 socket 还是当前 ws 时才重连（避免 disconnect() 后误触发重连）
      if (ws.value === socket) {
        scheduleReconnect()
      }
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
    stopHeartbeat()
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    isReconnecting = false
    const cur = ws.value
    ws.value = null  // 先置 null，阻止 onclose 触发重连
    cur?.close()
    connected.value = false
  }

  function updateMessageRecall(messageId: number) {
    currentMessages.value = currentMessages.value.map(m =>
      m.id === messageId ? { ...m, deleted: true, content: '消息已撤回' } : m
    )
  }

  function updateConversation(msg: ChatMessage) {
    const myId = Number(localStorage.getItem('userId'))
    const otherId = msg.senderId === myId ? msg.receiverId : msg.senderId
    const idx = conversations.value.findIndex(c => c.userId === otherId)
    const isInvite = msg.msgType === 4 || (msg.content && String(msg.content).includes('INVITE#'))
    const lastMsg = isInvite ? '[邀约邀请]' : (msg.content ?? '')
    const dismissStore = useNotifyDismissStore()
    if (idx >= 0) {
      const conv = conversations.value[idx]
      if (conv) {
        conv.lastMessage = lastMsg
        conv.lastTime = msg.createdAt ?? ''
        conv.msgType = msg.msgType
      }
      dismissStore.undismissChat(otherId)
    } else {
      dismissStore.undismissChat(otherId)
      fetchConversations()
    }
  }

  return {
    conversations,
    currentMessages,
    pendingByUser,
    connected,
    fetchConversations,
    connectWebSocket,
    updateMessageRecall,
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
