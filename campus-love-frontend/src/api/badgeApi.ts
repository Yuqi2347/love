import request from './request'
import type { ApiResult } from './request'

export interface BadgeCounts {
  unreadMessageCount: number
  newFollowerCount: number
  newFeedActivityCount: number
  newInviteActivityCount: number
}

export function getBadges() {
  return request.get<ApiResult<BadgeCounts>>('/user/badges')
}

export function markFollowersViewed() {
  return request.put<ApiResult<void>>('/follow/mark-followers-viewed')
}

export function markFeedActivityViewed() {
  return request.put<ApiResult<void>>('/feed/activity/mark-viewed')
}

export function markInviteActivityViewed() {
  return request.put<ApiResult<void>>('/invite/activity/mark-viewed')
}
