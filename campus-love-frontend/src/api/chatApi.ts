import request from './request'
import type { ApiResult } from './request'

export interface ChatMessage {
  id: number
  senderId: number
  receiverId: number
  groupId?: number | null
  senderNickname: string
  senderAvatar: string | null
  content: string
  msgType: number
  isRead: boolean
  createdAt: string
}

export interface Conversation {
  userId: number
  nickname: string
  avatarUrl: string | null
  lastMessage: string
  lastTime: string
  unreadCount: number
}

export function getConversations() {
  return request.get<ApiResult<Conversation[]>>('/chat/conversations')
}

export function getChatHistory(otherUserId: number, page = 0, size = 20) {
  return request.get<ApiResult<ChatMessage[]>>(`/chat/history/${otherUserId}`, { params: { page, size } })
}

export function markAsRead(otherUserId: number) {
  return request.put<ApiResult<void>>(`/chat/read/${otherUserId}`)
}
