import request from './request'
import type { ApiResult } from './request'

export const VIOLATION_TYPES = [
  { value: 'spam', label: '垃圾广告' },
  { value: 'harassment', label: '骚扰辱骂' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'fake', label: '虚假信息' },
  { value: 'other', label: '其他' },
] as const

export function checkReported(targetType: string, targetId: number) {
  return request.get<ApiResult<boolean>>('/report/check', { params: { targetType, targetId } })
}

export function getMyReport(targetType: string, targetId: number) {
  return request.get<ApiResult<{
    id: number
    violationTypes: string
    reason: string | null
    adminNote: string | null
  } | null>>('/report/my', { params: { targetType, targetId } })
}

export function submitReport(data: { targetType: string; targetId: number; violationTypes: string[]; reason?: string }) {
  return request.post<ApiResult<void>>('/report', data)
}

/** 管理员：按帖子ID获取举报数量 */
export function getReportCountByPostIds(postIds: number[]) {
  if (!postIds.length) return Promise.resolve({ data: { data: {} as Record<string, number> } })
  const query = postIds.map((id) => `postIds=${id}`).join('&')
  return request.get<ApiResult<Record<string, number>>>(`/report/count-by-post?${query}`)
}

export interface ReportItem {
  id: number
  reporterId: number
  reporterNickname: string
  targetType: string
  targetId: number
  targetSummary: string
  violationTypes: string
  reason: string | null
  status: string
  adminNote: string | null
  createdAt: string
  reviewedAt: string | null
}

/** 管理员：举报列表 */
export function listReports(params: { page?: number; size?: number; status?: string; targetId?: number }) {
  return request.get<ApiResult<ReportItem[]>>('/report/list', { params })
}

/** 管理员：审核举报 */
export function reviewReport(id: number, data: { adminNote?: string; status?: string }) {
  return request.put<ApiResult<void>>(`/report/${id}/review`, data)
}
