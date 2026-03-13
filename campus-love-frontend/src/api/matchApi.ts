import request from './request'
import type { ApiResult } from './request'

export interface MatchDetail {
  interestScore: number
  mbtiScore: number
  zodiacScore: number
  /** 任一方 bazi_unknown 时为 null，前端展示「暂无数据」 */
  baziScore: number | null
  majorScore: number
  ageScore: number
  /** V1.1.0：OCEAN 契合度，无真实 OCEAN 时为 null */
  oceanScore?: number | null
}

export interface MatchResult {
  userId: number
  nickname: string
  avatarUrl: string | null
  gender: number
  school: string | null
  major: string | null
  grade: string | null
  mbti: string | null
  zodiac: string | null
  bio: string | null
  interests: string | null
  matchScore: number
  detail: MatchDetail
  /** V1.1.0：AI 一句话总结，前端展示用，不展示总分 */
  aiSummary?: string
}

export function getRecommendations(page = 0, size = 10, genderFilter: 'all' | 'same' | 'opposite' = 'all') {
  return request.get<ApiResult<MatchResult[]>>('/match/recommendations', { params: { page, size, genderFilter } })
}

export function getMatchDetail(targetUserId: number) {
  return request.get<ApiResult<MatchResult>>(`/match/detail/${targetUserId}`)
}

// 用户行为上报接口（V2.0动态权重）
export function reportUserAction(targetUserId: number, actionType: 'FOLLOW' | 'IGNORE' | 'CHAT_INIT' | 'BLOCK' | 'PROFILE_VIEW') {
  return request.post<ApiResult<void>>('/match/action', {
    targetUserId,
    actionType
  })
}

// 获取用户权重统计
export function getWeightStats() {
  return request.get<ApiResult<{
    userId: number
    actionCount: number
    canUsePersonalizedWeights: boolean
    weights: Record<string, number>
    source: 'personalized' | 'default'
    lastUpdated: string
  }>>('/match/weights/stats')
}

// 更新用户权重偏好 (high/medium/low)
export function updateWeightPreferences(preferences: Record<string, 'high' | 'medium' | 'low'>) {
  return request.post<ApiResult<void>>('/match/weights/preferences', preferences)
}

// 重置用户权重
export function resetWeights() {
  return request.post<ApiResult<void>>('/match/weights/reset')
}

// 获取支持的行为类型
export function getActionTypes() {
  return request.get<ApiResult<{
    actionTypes: Array<{
      name: string
      description: string
      signalStrength: number
    }>
  }>>('/match/actions/types')
}
