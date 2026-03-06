import request from './request'
import type { ApiResult } from './request'

export interface UserProfile {
  id: number
  email: string
  nickname: string
  gender: number
  birthDate: string | null
  birthTime: string | null
  school: string | null
  major: string | null
  grade: string | null
  activityScore: number
  userLevel: number
  isAdmin: boolean
  creditScore: number
  inviteCount: number
  participateCount: number
  mbti: string | null
  zodiac: string | null
  bazi: string | null
  avatarUrl: string | null
  bio: string | null
  interests: string | null
  profileComplete: boolean
}

export interface UpdateProfileParams {
  nickname: string
  gender: number
  birthDate: string
  birthTime?: string
  school?: string
  major?: string
  grade?: string
  mbti?: string
  bio?: string
  interests?: string
}

export function getMyProfile() {
  return request.get<ApiResult<UserProfile>>('/user/me')
}

export function getUserProfile(userId: number) {
  return request.get<ApiResult<UserProfile>>(`/user/${userId}`)
}

export interface UserSearchItem {
  id: number
  nickname: string
  avatarUrl: string | null
}

export function searchUsers(keyword: string, limit = 10) {
  return request.get<ApiResult<UserSearchItem[]>>('/user/search', {
    params: { keyword: keyword.trim(), limit },
  })
}

export function updateProfile(data: UpdateProfileParams) {
  return request.put<ApiResult<UserProfile>>('/user/profile', data)
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<ApiResult<string>>('/user/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
