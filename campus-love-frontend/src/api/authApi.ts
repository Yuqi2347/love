import request from './request'
import type { ApiResult } from './request'

export interface RegisterParams {
  email: string
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

export function register(data: RegisterParams) {
  return request.post<ApiResult<AuthResponse>>('/auth/register', data)
}

export function login(data: LoginParams) {
  return request.post<ApiResult<AuthResponse>>('/auth/login', data)
}
