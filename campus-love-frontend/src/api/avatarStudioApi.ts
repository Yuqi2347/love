import request from './request'
import type { ApiResult } from './request'

export interface AvatarStudioQuota {
  limit: number
  used: number
  remaining: number
}

export interface AvatarStudioGenerateResult {
  imageBase64: string
  mimeType: string
  remaining: number
}

export function getAvatarStudioQuota() {
  return request.get<ApiResult<AvatarStudioQuota>>('/user/avatar-studio/quota')
}

/** 图生图较慢，单独拉长超时 */
export function postAvatarStudioGenerate(file: File, style: string) {
  const form = new FormData()
  form.append('file', file)
  form.append('style', style)
  return request.post<ApiResult<AvatarStudioGenerateResult>>('/user/avatar-studio/generate', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000,
  })
}
