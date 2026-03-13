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
  // 第一步
  socialStyle: string
  lifeRhythm: string
  personalityBase: string
  campusFocus: string
  emotionStyle: string
  companionshipStyle: string
  // 第二步
  appearanceRequirement: string
  ageRangePreference?: string
  agePreferenceMin?: number
  agePreferenceMax?: number
  gradeRangePreference: string
  gradeRangeMin?: number
  gradeRangeMax?: number
  partnerPersonality: string
  majorPreference: string
  careerAmbitionPref: string
  dateStyle: string
  intimacyPace: string
  // 第三步
  honestyLevel: string
  premaritalCohabitation: string
  premaritalSex: string
  relationshipCoreValue: string
  conflictStyle: string
  socialBoundary: string
  futureLifestyle: string
  campusLovePlan: string
  idolRole: string
  temptationResponse: string
  realityCondition: string
  humanNatureView: string
  breakupView: string
  careerLoveConflict: string
  emotionPriority: string
  lifeGoalPriority: string
}

export interface MomentProfile {
  id: number
  userId: number
  targetGender: string
  socialStyle: string
  lifeRhythm: string
  personalityBase?: string
  campusFocus?: string
  emotionStyle?: string
  companionshipStyle: string
  appearanceRequirement: string
  ageRangePreference?: string
  agePreferenceMin?: number
  agePreferenceMax?: number
  gradeRangePreference?: string
  gradeRangeMin?: number
  gradeRangeMax?: number
  partnerPersonality: string
  majorPreference: string
  careerAmbitionPref?: string
  dateStyle: string
  intimacyPace: string
  honestyLevel?: string
  premaritalCohabitation: string
  premaritalSex?: string
  relationshipCoreValue: string
  conflictStyle?: string
  socialBoundary?: string
  futureLifestyle: string
  campusLovePlan?: string
  idolRole?: string
  temptationResponse?: string
  realityCondition?: string
  humanNatureView?: string
  breakupView?: string
  careerLoveConflict?: string
  emotionPriority?: string
  lifeGoalPriority?: string
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
  summary?: string | null
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
