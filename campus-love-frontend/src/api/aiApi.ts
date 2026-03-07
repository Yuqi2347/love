import request from './request'
import type { ApiResult } from './request'

export interface YuanFenAnalysisResult {
  yuanFenIndex: string
  personalityAnalysis: string
  recommendActivities: string[]
  potentialChallenge: string
  developmentPotential: string
  exclusiveQuote: string
  generatedAt: string
  nextAvailableAt: string
}

export function getYuanFenAnalysis(targetUserId: number) {
  return request.post<ApiResult<YuanFenAnalysisResult>>('/ai/yuanfen-analysis', { targetUserId })
}

export function getYuanFenCooldown(targetUserId: number) {
  return request.get<ApiResult<{ remainingSeconds: number }>>(`/ai/yuanfen-cooldown/${targetUserId}`)
}
