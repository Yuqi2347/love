import request from './request'
import type { ApiResult } from './request'

export interface AdminUserItem {
  id: number
  email: string
  nickname: string
  school: string | null
  status: number | null
  isAdmin: boolean
  creditScore: number | null
  activityScore: number | null
  userLevel: number | null
  inviteCount: number | null
  participateCount: number | null
  createdAt: string
}

export interface AdminInviteItem {
  id: number
  creatorId: number
  creatorNickname: string
  inviteType: string
  inviteMode: string
  title: string
  status: string
  inviteTime: string
  participantCount: number
  maxParticipants: number | null
  createdAt: string
}

export interface AdminFeedItem {
  id: number
  userId: number
  nickname: string
  content: string
  postType: string
  likeCount: number
  commentCount: number
  createdAt: string
}

export interface DashboardStats {
  userTotal: number
  inviteTotal: number
  feedTotal: number
  activeUsersToday: number
  activeUsers7d: number
  profileCompleteCount: number
  newUsersToday: number
  embeddingCount: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export function getAdminUsers(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<ApiResult<PageResult<AdminUserItem>>>('/admin/users', { params })
}

export function getAdminInvites(params: { page?: number; size?: number; status?: string }) {
  return request.get<ApiResult<PageResult<AdminInviteItem>>>('/admin/invites', { params })
}

export function deleteAdminInvite(id: number) {
  return request.delete<ApiResult<void>>(`/admin/invite/${id}`)
}

export function getAdminFeeds(params: { page?: number; size?: number; userId?: number }) {
  return request.get<ApiResult<PageResult<AdminFeedItem>>>('/admin/feed/list', { params })
}

export function deleteAdminFeed(id: number) {
  return request.delete<ApiResult<void>>(`/admin/feed/${id}`)
}

export function updateUserCredit(id: number, creditScore: number) {
  return request.put<ApiResult<void>>(`/admin/user/${id}/credit`, { creditScore })
}

export function updateUserStats(id: number, data: { creditScore?: number; activityScore?: number; userLevel?: number }) {
  return request.put<ApiResult<void>>(`/admin/user/${id}/stats`, data)
}

/** 彻底删除用户及其全部相关数据（不可恢复） */
export function deleteAdminUser(id: number) {
  return request.delete<ApiResult<void>>(`/admin/user/${id}`)
}

export function getDashboardStats() {
  return request.get<ApiResult<DashboardStats>>('/admin/stats')
}

// ==================== 举报管理 ====================

export interface ReportItem {
  id: number
  reporterId: number
  reporterNickname: string
  targetType: string
  targetId: number
  targetSummary: string
  violationTypes: string
  reason: string | null
  status: string
  adminNote: string | null
  createdAt: string
  reviewedAt: string | null
}

export function getReportList(params: { page?: number; size?: number; status?: string; targetId?: number }) {
  return request.get<ApiResult<ReportItem[]>>('/report/list', { params })
}

export function reviewReport(id: number, data: { adminNote?: string; status?: string }) {
  return request.put<ApiResult<void>>(`/report/${id}/review`, data)
}

// ==================== AI Token 统计 ====================

export interface AiTokenDailyStat {
  date: string
  tokensUsed: number
  callCount: number
}

export interface AiTokenStats {
  totalTokens: number
  callCount: number
  dailyStats: AiTokenDailyStat[]
}

export function getAiTokenStats(range: 'day' | 'week' | 'month' = 'week') {
  return request.get<ApiResult<AiTokenStats>>('/admin/ai/token-stats', { params: { range } })
}

// ==================== 心动时刻管理 ====================

export interface MomentStatusInfo {
  currentWeek: string
  status: string
  participantCount: number
  enrollmentOpen: boolean
}

export interface MomentEnrollmentItem {
  id: number
  userId: number
  weekTag: string
  pool: string
  status: string
  createdAt: string
}

export interface MomentMatchConfig {
  id: number
  baseThreshold: number
  prioritizeOffset: number
  priorityOffset: number
  priorityMaxStack: number
}

export interface MomentHistogramBucket {
  label: string
  count: number
}

export interface MomentPoolStat {
  pool: string
  participants: number
  matchedPairs: number
  unmatchedUsers: number
}

export interface MomentReasonStat {
  reason: string
  count: number
}

export interface MomentFilteredPairSample {
  pool: string
  userIdA: number
  userIdB: number
  score: number
  thresholdRequired: number
}

export interface MomentDashboardData {
  weekTag: string
  participantCount: number
  matchedUsers: number
  unmatchedUsers: number
  successRate: number
  currentThreshold: number
  matchedScoreHistogram: MomentHistogramBucket[]
  unmatchedBestScoreHistogram: MomentHistogramBucket[]
  filteredPairCount: number
  poolStats: MomentPoolStat[]
  hardFilterStats: MomentReasonStat[]
  softPenaltyStats: MomentReasonStat[]
  unmatchedReasonStats: MomentReasonStat[]
  filteredPairSamples: MomentFilteredPairSample[]
}

export interface MomentUnmatchedUser {
  userId: number
  nickname?: string | null
  pool: string
  highestAvailableScore?: number | null
  reason: string
  priorityCount: number
  prioritizeMatching: boolean
}

export interface MomentSimulationResponse {
  weekTag: string
  threshold: number
  matchedPairs: number
  matchedUsers: number
  unmatchedUsers: number
  successRate: number
  deltaPairs: number
}

export function getMomentStatus() {
  return request.get<ApiResult<MomentStatusInfo>>('/moment/status')
}

export function triggerMomentMatching(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/trigger', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function closeMomentEnrollment(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/close', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function reopenMomentEnrollment(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/reopen', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function resetMomentWeek(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/reset', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function getMomentMatchDashboard(weekTag?: string) {
  return request.get<ApiResult<MomentDashboardData>>('/moment/admin/dashboard', {
    params: weekTag ? { weekTag } : {},
  })
}

export function getMomentMatchUnmatched(weekTag?: string) {
  return request.get<ApiResult<MomentUnmatchedUser[]>>('/moment/admin/dashboard/unmatched', {
    params: weekTag ? { weekTag } : {},
  })
}

export function simulateMomentMatchDashboard(data: { weekTag: string; threshold: number }) {
  return request.post<ApiResult<MomentSimulationResponse>>('/moment/admin/dashboard/simulate', data)
}

export function getMomentMatchConfig() {
  return request.get<ApiResult<MomentMatchConfig>>('/moment/admin/config')
}

export function updateMomentMatchConfig(data: Omit<MomentMatchConfig, 'id'>) {
  return request.put<ApiResult<MomentMatchConfig>>('/moment/admin/config', data)
}
