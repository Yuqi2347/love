import request from './request'
import type { ApiResult } from './request'

// ==================== 类型定义 ====================

export interface MomentStatusResponse {
  currentWeek: string
  status: 'NOT_ENROLLED' | 'WAITING' | 'MATCHED' | 'UNMATCHED'
  participantCount: number
  enrollmentOpen: boolean
}

export interface MomentEnrollRequest {
  selfScore: number
  targetGender: 'male' | 'female' | 'any'
  socialStyle: string
  lifeRhythm: string
  companionshipStyle: string
  appearanceRequirement: string
  partnerPersonality: string
  majorPreference: string
  ageRangePreference: string
  dateStyle: string
  intimacyPace: string
  loyaltyValue: string
  premaritalCohabitation: string
  futureLifestyle: string
  relationshipCoreValue: string
}

export interface MomentProfile {
  id: number
  userId: number
  targetGender: string
  socialStyle: string
  lifeRhythm: string
  companionshipStyle: string
  appearanceRequirement: string
  partnerPersonality: string
  majorPreference: string
  ageRangePreference: string
  dateStyle: string
  intimacyPace: string
  loyaltyValue: string
  premaritalCohabitation: string
  futureLifestyle: string
  relationshipCoreValue: string
  momentPhotoUrl?: string | null
  momentSelfScore?: number | null
}

export interface MomentResultResponse {
  matched: boolean
  weekTag: string
  matchedUserId?: number
  nickname?: string
  avatarUrl?: string | null
  gender?: number
  school?: string | null
  major?: string | null
  grade?: string | null
  bio?: string | null
  mbti?: string | null
  zodiac?: string | null
  age?: number | null
  totalScore?: number
  scoreDetail?: {
    personality: number
    preference: number
    lifestyle: number
    coreValue: number
  }
}

// ==================== API 函数 ====================

export function getMomentStatus() {
  return request.get<ApiResult<MomentStatusResponse>>('/moment/status')
}

export function enrollMoment(data: MomentEnrollRequest) {
  return request.post<ApiResult<MomentStatusResponse>>('/moment/enroll', data)
}

export function getMomentResult() {
  return request.get<ApiResult<MomentResultResponse>>('/moment/result')
}

export function getMomentProfile() {
  return request.get<ApiResult<MomentProfile>>('/moment/profile')
}

export function uploadMomentPhoto(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResult<string>>('/moment/upload/photo', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
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
