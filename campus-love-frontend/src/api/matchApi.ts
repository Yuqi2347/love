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
