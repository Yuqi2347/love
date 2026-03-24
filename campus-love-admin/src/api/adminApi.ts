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

export interface AiTokenCategoryStat {
  tokensUsed: number
  callCount: number
}

export interface AiTokenDailyStat {
  date: string
  totalTokens: number
  callCount: number
  avatarTokens: number
  avatarCallCount: number
  analysisTokens: number
  analysisCallCount: number
}

export interface AiTokenStats {
  totalTokens: number
  callCount: number
  avatar: AiTokenCategoryStat
  analysis: AiTokenCategoryStat
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
  matchedTitle?: string | null
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
  /** 图匹配每人保留 eligible 边数 Top-K，默认 200 */
  eligibleTopK: number
  autoMatchEnabled: boolean
  autoMatchDayOfWeek: number
  autoMatchTime: string
  /** 流水线 RESULT_READY 后按北京时间自动公布给用户 */
  autoPublishEnabled: boolean
  autoPublishDayOfWeek: number
  autoPublishTime: string
}

export interface MomentAdminOverviewPoolStat {
  pool: string
  participants: number
  matchedPairs: number
  unmatchedUsers: number
}

export interface MomentAdminOverview {
  weekTag: string
  phase:
    | 'ENROLLING'
    | 'WAITING_MATCH'
    | 'MATCHING'
    | 'AI_ANALYZING'
    | 'RESULT_READY'
    | 'PUBLISHED'
    | 'FAILED'
  participantCount: number
  waitingUsers: number
  matchedUsers: number
  unmatchedUsers: number
  matchedPairs: number
  successRate: number
  enrollmentOpen: boolean
  currentThreshold: number
  autoMatchEnabled: boolean
  autoMatchDayOfWeek: number
  autoMatchTime: string
  nextAutoMatchAt?: string | null
  lastMatchAt?: string | null
  autoPublishEnabled?: boolean
  autoPublishDayOfWeek?: number
  autoPublishTime?: string
  /** RESULT_READY 且未到点时有值 */
  nextAutoPublishAt?: string | null
  poolStats: MomentAdminOverviewPoolStat[]
  canTriggerMatching: boolean
  canCloseEnrollment: boolean
  canReopenEnrollment: boolean
  canResetWeek: boolean
  /** RESULT_READY 时可公布给用户 */
  canPublishResult: boolean
}

export interface MomentMatchProgressSnapshot {
  currentPool: string
  processedPairs: number
  totalEstimatedPairs: number
  matchedPairs: number
}

export interface MomentMatchProgressResponse {
  status: string
  errorMessage?: string | null
  matchProgress: MomentMatchProgressSnapshot
  aiProgress: { total: number; done: number; failed: number }
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
  /** 当前配置的 Top-K */
  eligibleTopK: number
  /** 候选边类指标口径说明 */
  statsNote: string
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

export interface MomentEnrollmentAdminItem {
  weekTag: string
  userId: number
  nickname: string
  gender?: number | null
  school?: string | null
  major?: string | null
  grade?: string | null
  pools: string[]
  status: string
  prioritizeMatching: boolean
  priorityCount: number
  createdAt: string
}

export interface MomentMatchResultItem {
  id: number
  weekTag: string
  pool: string
  userIdA: number
  nicknameA: string
  userIdB: number
  nicknameB: string
  totalScore: number
  yuanfenTitle?: string | null
  choiceA?: string | null
  choiceB?: string | null
  confirmStatus: string
  createdAt: string
}

export interface MatchedUserCard {
  userId?: number | null
  nickname?: string | null
  gender?: number | null
  school?: string | null
  major?: string | null
  grade?: string | null
  mbti?: string | null
  zodiac?: string | null
  age?: number | null
}

export interface MomentDatePrepTopic {
  title: string
  opener: string
}

export interface MomentDatePrepInfo {
  dateSceneType?: string | null
  dateSuggestion?: string | null
  iceBreakTopics?: MomentDatePrepTopic[] | null
  surpriseIdea?: string | null
  outfitAdvice?: string | null
  mindsetAdvice?: string | null
}

export interface MomentMatchResultDetail {
  id: number
  weekTag: string
  pool: string
  totalScore: number
  scoreDetail?: string | null
  yuanfenTitle?: string | null
  complementaryModes?: string | null
  softPenaltyReasons?: string | null
  dateSceneType?: string | null
  insightCards: string[]
  goldenSentence?: string | null
  dimensionLabels?: string | null
  aboutUserA?: string | null
  aboutUserB?: string | null
  datePrepA?: MomentDatePrepInfo | null
  datePrepB?: MomentDatePrepInfo | null
  datePrepJson?: string | null
  userA: MatchedUserCard
  userB: MatchedUserCard
  choiceA?: string | null
  choiceB?: string | null
  choiceAAt?: string | null
  choiceBAt?: string | null
  confirmStatus: string
  createdAt: string
}

export interface MomentOperationLogItem {
  id: number
  weekTag?: string | null
  operatorId?: number | null
  operatorName: string
  actionType: string
  targetType: string
  targetId?: number | null
  summary?: string | null
  detailJson?: string | null
  createdAt: string
}

export function getMomentStatus() {
  return request.get<ApiResult<MomentStatusInfo>>('/moment/status')
}

export function triggerMomentMatching(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/trigger', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function publishMomentResult(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/publish', null, {
    params: weekTag ? { weekTag } : {},
  })
}

export function getMomentMatchProgress(weekTag?: string) {
  return request.get<ApiResult<MomentMatchProgressResponse>>('/moment/admin/match/progress', {
    params: weekTag ? { weekTag } : {},
  })
}

export function getMomentAdminOverview(weekTag?: string) {
  return request.get<ApiResult<MomentAdminOverview>>('/moment/admin/overview', {
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

export function getMomentAdminEnrollments(params: {
  page?: number
  size?: number
  weekTag?: string
  pool?: string
  gender?: number
  status?: string
  keyword?: string
}) {
  return request.get<ApiResult<PageResult<MomentEnrollmentAdminItem>>>('/moment/admin/enrollments', { params })
}

export function removeMomentAdminEnrollment(userId: number, weekTag?: string) {
  return request.delete<ApiResult<void>>(`/moment/admin/enrollments/user/${userId}`, {
    params: weekTag ? { weekTag } : {},
  })
}

export function getMomentAdminResults(params: {
  page?: number
  size?: number
  weekTag?: string
  pool?: string
  keyword?: string
}) {
  return request.get<ApiResult<PageResult<MomentMatchResultItem>>>('/moment/admin/results', { params })
}

export function getMomentAdminResultDetail(id: number) {
  return request.get<ApiResult<MomentMatchResultDetail>>(`/moment/admin/results/${id}`, {
    timeout: 60000,
  })
}

export function getMomentAdminLogs(params: {
  page?: number
  size?: number
  weekTag?: string
  actionType?: string
}) {
  return request.get<ApiResult<PageResult<MomentOperationLogItem>>>('/moment/admin/logs', { params })
}

// ==================== 全站公告 ====================

export interface SiteAnnouncementAdminItem {
  id: number
  title: string
  content: string
  status: string
  validFrom: string
  validUntil: string
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export function getAdminAnnouncements(params: { page?: number; size?: number }) {
  return request.get<ApiResult<PageResult<SiteAnnouncementAdminItem>>>('/admin/announcements', { params })
}

export function createAdminAnnouncement(data: {
  title: string
  content: string
  validFrom: string
  validUntil: string
  publish: boolean
}) {
  return request.post<ApiResult<SiteAnnouncementAdminItem>>('/admin/announcements', data)
}

export function updateAdminAnnouncement(
  id: number,
  data: { title: string; content: string; validFrom: string; validUntil: string },
) {
  return request.put<ApiResult<SiteAnnouncementAdminItem>>(`/admin/announcements/${id}`, data)
}

export function publishAdminAnnouncement(id: number) {
  return request.post<ApiResult<void>>(`/admin/announcements/${id}/publish`)
}

export function unpublishAdminAnnouncement(id: number) {
  return request.post<ApiResult<void>>(`/admin/announcements/${id}/unpublish`)
}

export function deleteAdminAnnouncement(id: number) {
  return request.delete<ApiResult<void>>(`/admin/announcements/${id}`)
}
