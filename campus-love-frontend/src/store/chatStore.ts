import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage, Conversation } from '@/api/chatApi'
import { getConversations } from '@/api/chatApi'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentMessages = ref<ChatMessage[]>([])
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
    const wsUrl = `${protocol}//${window.location.host}/ws/chat?token=${token}`
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => { connected.value = true }

    ws.value.onmessage = (event: MessageEvent) => {
      const msg: ChatMessage = JSON.parse(event.data)
      currentMessages.value.push(msg)
      updateConversation(msg)
    }

    ws.value.onclose = () => {
      connected.value = false
      setTimeout(connectWebSocket, 3000)
    }
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
    conversations, currentMessages, connected,
    fetchConversations, connectWebSocket, sendMessage, sendGroupMessage, disconnect,
  }
})
