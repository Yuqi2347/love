import request from './request'
import type { ApiResult } from './request'

export interface AdminUserItem {
  id: number
  email: string
  nickname: string
  school: string | null
  status: number | null
  isAdmin: boolean
  creditScore: number | null
  userLevel: number | null
  inviteCount: number | null
  participateCount: number | null
  createdAt: string
}

export interface AdminInviteItem {
  id: number
  creatorId: number
  creatorNickname: string
  inviteType: string
  inviteMode: string
  title: string
  status: string
  inviteTime: string
  participantCount: number
  maxParticipants: number | null
  createdAt: string
}

export interface DashboardStats {
  userTotal: number
  inviteTotal: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export function getAdminUsers(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<ApiResult<PageResult<AdminUserItem>>>('/admin/users', { params })
}

export function getAdminInvites(params: { page?: number; size?: number; status?: string }) {
  return request.get<ApiResult<PageResult<AdminInviteItem>>>('/admin/invites', { params })
}

export function getDashboardStats() {
  return request.get<ApiResult<DashboardStats>>('/admin/stats')
}
