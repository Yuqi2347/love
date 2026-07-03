import axios from 'axios'
import request from './request'
import type { ApiResult } from './request'

export interface SchoolItem {
  name: string
  domain: string
  /** 邮箱后缀，用于注册校验（如 szu.edu.cn） */
  emailSuffix?: string
  /** 可选：完整域名列表，存在时注册页可「学号 + @域名」拼接（如深圳大学双后缀） */
  emailDomains?: string[]
  /** 学校可选校区列表 */
  campuses?: string[]
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

function isRetryableSchoolSearchError(err: unknown): boolean {
  if (!axios.isAxiosError(err)) return false
  if (!err.response) return true
  const s = err.response.status
  return s >= 500 && s < 600
}

/**
 * 学校列表来自服务端内存 JSON，不走数据库。
 * 生产环境偶发 net::ERR_CONNECTION_CLOSED 时自动重试，减轻 Nginx/连接复用抖动影响。
 */
export async function searchSchools(keyword?: string) {
  const params = { keyword }
  let lastErr: unknown
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      return await request.get<ApiResult<SchoolItem[]>>('/auth/schools', { params })
    } catch (e) {
      lastErr = e
      if (!isRetryableSchoolSearchError(e) || attempt === 2) throw lastErr
      await new Promise((r) => setTimeout(r, 280 * (attempt + 1)))
    }
  }
  throw lastErr
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
