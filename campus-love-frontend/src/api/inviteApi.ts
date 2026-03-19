import service from './request'
import type { ApiResult } from './request'

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
  inviteEndTime?: string | null
  location: string | null
  campus?: string | null
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
  /** 1v1 专属邀约的被邀人信息（仅 inviteMode=PRIVATE 时有值） */
  targetUser?: CreatorInfo | null
  participants?: ParticipantInfo[]
  /** 公开邀约的群聊 ID，用于详情页内嵌聊天 */
  chatGroupId?: number | null
  /** 当前用户角色：CREATOR 发起 / PARTICIPANT 参与中 / LEFT 已退出 */
  myRole?: string
  /** 当前用户退出时间（myRole=LEFT 时有值） */
  myLeftAt?: string
  createdAt: string
}

export interface InviteTypeCount {
  inviteType: string
  count: number
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
  inviteEndTime?: string
  location?: string
  campus?: string
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

/** 待处理的再次加入申请（发起人视角） */
export interface InviteRejoinRequestItem {
  userId: number
  nickname: string | null
  avatarUrl: string | null
  requestedAt: string
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
export function getInviteList(type?: string, status?: string, timeRange?: string, keyword?: string, publicOnly?: boolean, page = 1, size = 20) {
  return service.get<ApiResult<{ records: Invite[]; total: number; current: number; size: number }>>('/invite/list', { params: { type, status, timeRange, keyword, publicOnly, page, size } })
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
 * 拒绝邀约（仅一对一邀约的被邀方）
 */
export function declineInvite(id: number) {
  return service.post<ApiResult<void>>(`/invite/${id}/decline`)
}

/**
 * 申请再次加入邀约（已退出用户）
 */
export function requestRejoin(id: number) {
  return service.post<ApiResult<void>>(`/invite/${id}/rejoin-request`)
}

/**
 * 发起人：获取待处理的再次加入申请列表
 */
export function getRejoinRequests(id: number) {
  return service.get<ApiResult<InviteRejoinRequestItem[]>>(`/invite/${id}/rejoin-requests`)
}

/**
 * 发起人：同意某人再次加入
 */
export function approveRejoin(inviteId: number, userId: number) {
  return service.post<ApiResult<void>>(`/invite/${inviteId}/rejoin-approve/${userId}`)
}

/**
 * 发起人：拒绝某人再次加入
 */
export function rejectRejoin(inviteId: number, userId: number) {
  return service.post<ApiResult<void>>(`/invite/${inviteId}/rejoin-reject/${userId}`)
}

/**
 * 发起人：踢出参与者（必须填写至少10字理由）
 */
export function kickParticipant(inviteId: number, userId: number, reason: string) {
  return service.post<ApiResult<void>>(`/invite/${inviteId}/kick/${userId}`, { reason })
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
 * 获取指定用户的邀约统计
 */
export function getUserInviteStats(userId: number) {
  return service.get<ApiResult<InviteStats>>(`/invite/user/${userId}/stats`)
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
 * 我的邀约列表（我发起的 + 我参与的，含已退出，按邀约时间倒序）
 */
export function getMyInvitesList(range: string = 'week') {
  const rangeParam = range === 'year' ? 'all' : range
  return service.get<ApiResult<Invite[]>>('/invite/my-list', { params: { range: rangeParam } })
}

/**
 * 获取推荐邀约列表
 */
export function getRecommendInvites(limit = 10) {
  return service.get<ApiResult<Invite[]>>('/invite/recommend', { params: { limit } })
}

/**
 * 热门邀约看板：按类型统计邀约中数量（所有用户可见）
 */
export function getHotInviteTypeCounts(limit = 10) {
  return service.get<ApiResult<InviteTypeCount[]>>('/invite/board/type-counts', { params: { limit } })
}
