import request from './request'
import type { ApiResult } from './request'

export interface SiteAnnouncement {
  id: number
  title: string
  content: string
  validFrom: string
  validUntil: string
  publishedAt: string | null
}

/** 当前有效期内、未读的已发布公告（最多 50 条） */
export function getUnreadAnnouncements() {
  return request.get<ApiResult<SiteAnnouncement[]>>('/user/announcements/unread')
}

/** 关闭浮窗时批量标记已读 */
export function dismissAnnouncements(ids: number[]) {
  return request.post<ApiResult<number>>('/user/announcements/dismiss', { ids })
}
