import request from './request'
import type { ApiResult } from './request'

// ==================== 类型定义 ====================

export interface MomentStatusResponse {
  currentWeek: string
  status: 'NOT_ENROLLED' | 'WAITING' | 'MATCHED' | 'UNMATCHED'
  participantCount: number
  enrollmentOpen: boolean
  matchedTitle?: string | null
  /** 与后端 t_moment_activity_week.status 一致；用户可看详情需 PUBLISHED */
  weekStatus?: string | null
  /** 本期预计周五 12:00（北京时间）揭晓时刻，毫秒时间戳；实际以管理员发布为准 */
  revealAtEpochMillis?: number | null
}

export interface MomentEnrollRequest {
  selfScore: number
  targetGender: 'male' | 'female' | 'any'
  prioritizeMatching?: boolean
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
  prioritizeMatching?: boolean
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
  yuanfenTitle?: string | null
  /** 后端 t_moment_match_result.id，用于「约一下」协商 */
  matchResultId?: number | null
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
  complementaryModes?: string[]
  insightCards?: string[]
  goldenSentence?: string | null
  dimensionLabels?: string[]
  aboutMatchedUser?: string | null
  confirmStatus?: 'PENDING' | 'BOTH_YUE' | 'ANY_GUANZHU' | 'TIMEOUT_GUANZHU'
  myChoice?: 'YUE' | 'GUANZHU' | null
  datePrepUnlocked?: boolean
  /** 综合匹配度 0–100 */
  matchScorePercent?: number | null
}

export interface MomentDatePrepTopic {
  title: string
  opener: string
}

export interface NearbyShopItem {
  name: string
  address?: string
  typeName?: string
  distance?: string
}

export interface MomentDatePrepResponse {
  dateSceneType?: string
  dateSuggestion: string
  iceBreakTopics: MomentDatePrepTopic[]
  surpriseIdea: string
  outfitAdvice: string
  mindsetAdvice: string
  nearbyShops?: NearbyShopItem[]
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

export function confirmMomentChoice(choice: 'YUE' | 'GUANZHU') {
  return request.post<ApiResult<MomentResultResponse>>('/moment/result/confirm', { choice })
}

export function getMomentDatePrep() {
  return request.get<ApiResult<MomentDatePrepResponse>>('/moment/result/date-prep')
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

/** 已有结果（或失败后）整期重跑：归档旧结果、清空流水线、报名恢复待匹配后再次异步匹配（不删报名） */
export function rematchMomentWeek(weekTag?: string) {
  return request.post<ApiResult<Record<string, unknown>>>('/moment/admin/rematch', null, {
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
