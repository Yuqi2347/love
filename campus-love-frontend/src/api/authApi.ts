import request from './request'
import type { ApiResult } from './request'

export interface SchoolItem {
  name: string
  domain: string
  /** 邮箱后缀，用于注册校验（如 szu.edu.cn） */
  emailSuffix?: string
}

export interface RegisterParams {
  email: string
  verifyCode: string
  password: string
  nickname: string
  school?: string
}

export interface LoginParams {
  email: string
  password: string
}

export interface AuthResponse {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  profileComplete: boolean
  accessToken: string
  refreshToken: string
}

export interface PublicStatsResponse {
  activeUserCount: number
}

export function getPublicStats() {
  return request.get<ApiResult<PublicStatsResponse>>('/auth/stats')
}

export function searchSchools(keyword?: string) {
  return request.get<ApiResult<SchoolItem[]>>('/auth/schools', { params: { keyword } })
}

export function sendVerifyCode(email: string) {
  return request.post<ApiResult<void>>('/auth/send-verify-code', null, { params: { email } })
}

export function register(data: RegisterParams) {
  return request.post<ApiResult<AuthResponse>>('/auth/register', data)
}

export function login(data: LoginParams) {
  return request.post<ApiResult<AuthResponse>>('/auth/login', data)
}
