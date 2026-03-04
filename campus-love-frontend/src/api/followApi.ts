import request from './request'
import type { ApiResult } from './request'

export interface FollowUser {
  userId: number
  nickname: string
  avatarUrl: string | null
  isMutual: boolean
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
