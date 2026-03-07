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

export function getYuanFenAnalysis(targetUserId: number) {
  return request.post<ApiResult<YuanFenAnalysisResult>>('/ai/yuanfen-analysis', { targetUserId })
}

export function getYuanFenCooldown(targetUserId: number) {
  return request.get<ApiResult<{ remainingSeconds: number }>>(`/ai/yuanfen-cooldown/${targetUserId}`)
}
