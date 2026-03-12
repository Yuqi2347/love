import request from './request'
import type { ApiResult } from './request'

export interface FollowUser {
  userId: number
  nickname: string
  avatarUrl: string | null
  isMutual: boolean
  remark?: string // 备注名
}

export function followUser(targetUserId: number) {
  return request.post<ApiResult<void>>(`/follow/${targetUserId}`)
}

export function unfollowUser(targetUserId: number) {
  return request.delete<ApiResult<void>>(`/follow/${targetUserId}`)
}

export function getFollowStatus(targetUserId: number) {
  return request.get<ApiResult<string>>(`/follow/status/${targetUserId}`)
}

export function getFollowingList() {
  return request.get<ApiResult<FollowUser[]>>('/follow/following')
}

export function getFollowerList() {
  return request.get<ApiResult<FollowUser[]>>('/follow/followers')
}

export function getUserFollowing(userId: number) {
  return request.get<ApiResult<FollowUser[]>>(`/follow/user/${userId}/following`)
}

export function getUserFollowers(userId: number) {
  return request.get<ApiResult<FollowUser[]>>(`/follow/user/${userId}/followers`)
}

// 设置关注用户的备注名
export function setUserRemark(targetUserId: number, remark: string) {
  return request.put<ApiResult<void>>(`/follow/${targetUserId}/remark`, null, {
    params: { remark }
  })
}

// 获取用户的显示名称（优先显示备注，其次昵称）
export function getUserDisplayName(user: FollowUser | null | undefined): string {
  if (!user) return ''
  return user.remark && user.remark.trim() ? user.remark : user.nickname
}

// 获取互关朋友列表（用于分享帖子等场景）
export function getMutualFriends() {
  return request.get<ApiResult<FollowUser[]>>('/follow/mutual')
}
