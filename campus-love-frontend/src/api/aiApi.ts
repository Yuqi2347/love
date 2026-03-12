import request from './request'
import type { ApiResult } from './request'

export interface YuanFenAnalysisResult {
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

/** 缘分分析调用 AI 生成长文本，需更长超时（90 秒） */
export function getYuanFenAnalysis(targetUserId: number) {
  return request.post<ApiResult<YuanFenAnalysisResult>>('/ai/yuanfen-analysis', { targetUserId }, { timeout: 90000 })
}

export function getYuanFenCooldown(targetUserId: number) {
  return request.get<ApiResult<{ remainingSeconds: number }>>(`/ai/yuanfen-cooldown/${targetUserId}`)
}
