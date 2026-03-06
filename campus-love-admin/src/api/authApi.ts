import request from './request'
import type { ApiResult } from './request'

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
  isAdmin?: boolean
  accessToken: string
  refreshToken: string
}

export function login(data: LoginParams) {
  return request.post<ApiResult<AuthResponse>>('/auth/login', data)
}
