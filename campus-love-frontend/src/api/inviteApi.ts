import service from './request'
import type { ApiResult } from './request'
import { InviteStatus } from '@/constants/inviteConst'

// ==================== 类型定义 ====================

export interface CreatorInfo {
  id: number
  nickname: string
  avatarUrl: string | null
  creditScore: number | null
}

export interface ParticipantInfo {
  userId: number
  nickname: string
  avatarUrl: string | null
  joinAt: string
}

export interface Invite {
  id: number
  creatorId: number
  inviteType: string
  inviteMode: string
  targetUserId: number | null
  title: string
  content: string | null
  invitePeriod: string
  periodConfig: string | null
  inviteTime: string
  location: string | null
  maxParticipants: number | null
  participantCount: number
  status: string
  deadlineHours: number | null
  atmosphereTags: string | null
  isUrgent: boolean
  socialRating: number | null
  orgRating: number | null
  ratingCount: number
  creator: CreatorInfo | null
  participants?: ParticipantInfo[]
  createdAt: string
}

export interface InviteCreateRequest {
  inviteMode: 'PUBLIC' | 'PRIVATE'
  targetUserId?: number
  inviteType: string
  title: string
  content?: string
  invitePeriod: 'ONCE' | 'WEEKLY' | 'MONTHLY'
  periodConfig?: string
  inviteTime: string
  location?: string
  maxParticipants?: number
  deadlineHours?: number
  atmosphereTags?: string
  isUrgent?: boolean
}

export interface InviteWaitCreateRequest {
  inviteTypes: string[]
  periodConfig?: string
  locationPref?: string
  autoAccept?: boolean
  expireHours: number
  remark?: string
}

export interface InviteWait {
  id: number
  inviteTypes: string
  periodConfig: string | null
  locationPref: string | null
  autoAccept: boolean
  expireHours: number
  createdAt: string
  isExpired: boolean
  expireTime: string
}

export interface InviteRatingCreateRequest {
  inviteId: number
  ratedUserId: number
  socialRating: number
  orgRating?: number
  content?: string
}

export interface InviteStats {
  inviteCount: number
  participateCount: number
  successRate: number
  avgSocialRating: number | null
  avgOrgRating: number | null
  receivedSocialRating: number | null
  receivedOrgRating: number | null
}

export type HistoryRange = 'week' | 'month' | 'all'

// ==================== API 方法 ====================

/**
 * 发起邀约
 */
export function createInvite(data: InviteCreateRequest) {
  return service.post<ApiResult<number>>('/invite', data)
}

/**
 * 获取邀约列表
 */
export function getInviteList(type?: string, status?: string, page = 0, size = 20) {
  return service.get<ApiResult<any>>('/invite/list', { params: { type, status, page, size } })
}

/**
 * 获取邀约详情
 */
export function getInviteDetail(id: number) {
  return service.get<ApiResult<Invite>>(`/invite/${id}`)
}

/**
 * 加入邀约
 */
export function joinInvite(id: number) {
  return service.post<ApiResult<void>>(`/invite/${id}/join`)
}

/**
 * 退出邀约
 */
export function leaveInvite(id: number) {
  return service.delete<ApiResult<void>>(`/invite/${id}/leave`)
}

/**
 * 取消邀约
 */
export function cancelInvite(id: number, reason?: string) {
  return service.delete<ApiResult<void>>(`/invite/${id}/cancel`, { params: { reason } })
}

/**
 * 确认参与者
 */
export function confirmParticipants(id: number, userIds: number[]) {
  return service.put<ApiResult<void>>(`/invite/${id}/confirm`, { userIds })
}

/**
 * 创建等待邀约
 */
export function createInviteWait(data: InviteWaitCreateRequest) {
  return service.post<ApiResult<number>>('/invite/wait/create', data)
}

/**
 * 获取我的等待邀约
 */
export function getMyInviteWaits() {
  return service.get<ApiResult<InviteWait[]>>('/invite/wait/list')
}

/**
 * 取消等待邀约
 */
export function cancelInviteWait(id: number) {
  return service.delete<ApiResult<void>>(`/invite/wait/${id}`)
}

/**
 * 创建评价
 */
export function createRating(data: InviteRatingCreateRequest) {
  return service.post<ApiResult<void>>(`/invite/${data.inviteId}/rating`, data)
}

/**
 * 获取我的邀约统计
 */
export function getInviteStats() {
  return service.get<ApiResult<InviteStats>>('/invite/stats')
}

/**
 * 获取我发起的邀约历史
 */
export function getMyCreatedInvites(range: HistoryRange = 'week') {
  return service.get<ApiResult<Invite[]>>('/invite/history/created', { params: { range } })
}

/**
 * 获取我参与的邀约历史
 */
export function getMyJoinedInvites(range: HistoryRange = 'week') {
  return service.get<ApiResult<Invite[]>>('/invite/history/joined', { params: { range } })
}

/**
 * 获取推荐邀约列表
 */
export function getRecommendInvites(limit = 10) {
  return service.get<ApiResult<Invite[]>>('/invite/recommend', { params: { limit } })
}
