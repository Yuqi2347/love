import request from './request'
import type { ApiResult } from './request'

export interface SchoolItem {
  name: string
  domain: string
}

export interface RegisterParams {
  email: string
  verifyCode: string
  password: string
  nickname: string
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
