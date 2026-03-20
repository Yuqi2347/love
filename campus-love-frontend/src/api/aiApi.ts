import request from './request'
import type { ApiResult } from './request'

export interface YuanFenAnalysisResult {
  /** 后端返回：true 表示来自 t_yuanfen_analysis_log，未重新调 AI */
  fromCache?: boolean
  yuanFenIndex: string
  overallInterpretation?: string
  personalityAnalysis?: string
  personalityInteraction?: string  // 同性版本
  interestChemistry?: string
  campusStoryScene?: string
  campusMoment?: string           // 同性版本
  recommendActivities: string[]
  potentialChallenge: string
  developmentPotential?: string
  relationshipPotential?: string // 同性版本
  exclusiveQuote: string
  generatedAt?: string
  nextAvailableAt?: string
}

/** 与后端缘分专用 AI 超时、前端 axios 超时一致 */
export const YUANFEN_REQUEST_TIMEOUT_SEC = 60

/** 缘分分析调用 AI 生成长文本，需更长超时 */
export function getYuanFenAnalysis(targetUserId: number) {
  return request.post<ApiResult<YuanFenAnalysisResult>>(
    '/ai/yuanfen-analysis',
    { targetUserId },
    { timeout: YUANFEN_REQUEST_TIMEOUT_SEC * 1000 },
  )
}

export function getYuanFenCooldown(targetUserId: number) {
  return request.get<ApiResult<{ remainingSeconds: number }>>(`/ai/yuanfen-cooldown/${targetUserId}`)
}
