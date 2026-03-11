import request from './request'
import type { ApiResult } from './request'

export function submitReport(data: { targetType: string; targetId: number; reason: string }) {
  return request.post<ApiResult<void>>('/report', data)
}
