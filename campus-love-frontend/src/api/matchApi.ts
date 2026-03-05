import request from './request'
import type { ApiResult } from './request'

export interface MatchDetail {
  interestScore: number
  mbtiScore: number
  zodiacScore: number
  baziScore: number
  majorScore: number
  ageScore: number
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
}

export function getRecommendations(page = 0, size = 10) {
  return request.get<ApiResult<MatchResult[]>>('/match/recommendations', { params: { page, size } })
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
