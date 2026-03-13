import request from './request'
import type { ApiResult } from './request'

export interface ProfileStats {
  totalUsers: number
  profileCompleteCount: number
  hasProfileCount: number
  missingCount: number
}

/** 人物画像统计 */
export function getProfileStats() {
  return request.get<ApiResult<ProfileStats>>('/admin/profile/stats')
}

/** 手动触发单个用户画像生成 */
export function regenerateProfile(userId: number, force = false) {
  return request.post<ApiResult<void>>(`/admin/profile/regenerate/${userId}`, null, {
    params: { force },
  })
}

/** 批量补充缺失画像 */
export function batchRegenerateMissing() {
  return request.post<ApiResult<number>>('/admin/profile/batch-regenerate')
}
