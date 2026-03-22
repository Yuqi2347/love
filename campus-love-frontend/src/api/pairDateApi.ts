import request from './request'
import type { ApiResult } from './request'

export interface DateOptionItem {
  rank: number
  title: string
  description: string
  reason: string
}

export interface PairDateNegotiationVO {
  id?: number
  matchResultId?: number
  status: string
  weekTag?: string
  dateOptions?: { options: DateOptionItem[] }
  iAmUserA?: boolean
  myExcludedRank?: number | null
  myTimeSlots?: string[]
  myLocationChoice?: string | null
  partnerFinishedAll?: boolean
  revealPartnerChoices?: boolean | null
  partnerExcludedRank?: number | null
  partnerTimeSlots?: string[]
  partnerLocationChoice?: string | null
  finalDateOption?: DateOptionItem | null
  meetingTimeSlot?: string | null
  meetingTimestamp?: number | null
  locationDeciderId?: number | null
  deciderReasonKey?: string | null
  timeMismatch?: boolean | null
  /** 一对一邀约详情页 */
  pairInviteId?: number | null
  initiatorNickname?: string | null
  guestNickname?: string | null
  guestUserId?: number | null
  initiatorAvatarUrl?: string | null
  guestAvatarUrl?: string | null
}

export interface PairDateSubmitBody {
  step: 1 | 2 | 3
  excludedRank?: number
  timeSlots?: string[]
  locationChoice?: 'SELF' | 'PARTNER' | 'EITHER'
}

export interface PairDateTimeVO {
  meetingTimestamp: number | null
  serverTime: number
}

export function postPairDateYue(matchResultId: number) {
  return request.post<ApiResult<PairDateNegotiationVO>>('/pair-date/yue', { matchResultId })
}

export function getPairDate(negotiationId: number) {
  return request.get<ApiResult<PairDateNegotiationVO>>(`/pair-date/${negotiationId}`)
}

export function getPairDateByTarget(targetUserId: number) {
  return request.get<ApiResult<PairDateNegotiationVO | null>>(`/pair-date/pair/${targetUserId}`)
}

export function submitPairDateStep(negotiationId: number, body: PairDateSubmitBody) {
  return request.post<ApiResult<PairDateNegotiationVO>>(`/pair-date/${negotiationId}/submit`, body)
}

export function getPairDateTime(negotiationId: number) {
  return request.get<ApiResult<PairDateTimeVO>>(`/pair-date/${negotiationId}/time`)
}
