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
  deleted?: boolean
  createdAt: string
}

export interface Conversation {
  userId: number
  nickname: string
  avatarUrl: string | null
  lastMessage: string
  lastTime: string
  unreadCount: number
  msgType?: number
}

export function getConversations() {
  return request.get<ApiResult<Conversation[]>>('/chat/conversations')
}

/** 检查是否可向对方发送消息（未互关时仅允许发一条，对方回复前不能继续发） */
export function canSendTo(otherUserId: number) {
  return request.get<ApiResult<boolean>>(`/chat/can-send/${otherUserId}`)
}

export function getChatHistory(otherUserId: number, page = 0, size = 20) {
  return request.get<ApiResult<ChatMessage[]>>(`/chat/history/${otherUserId}`, { params: { page, size } })
}

export function markAsRead(otherUserId: number) {
  return request.put<ApiResult<void>>(`/chat/read/${otherUserId}`)
}

export function recallMessage(messageId: number) {
  return request.delete<ApiResult<void>>(`/chat/message/${messageId}`)
}

/** 邀约详情内嵌群聊历史（仅群成员可拉取） */
export function getGroupChatHistory(groupId: number, page = 1, size = 20) {
  return request.get<ApiResult<ChatMessage[]>>(`/chat/group/${groupId}/history`, { params: { page, size } })
}

/** 上传聊天图片，返回图片 URL（msgType=3） */
export function uploadChatImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<ApiResult<string>>('/chat/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

