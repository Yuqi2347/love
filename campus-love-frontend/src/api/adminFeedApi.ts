import request from './request'
import type { ApiResult } from './request'

/**
 * 管理端动态操作（需管理员 token）。
 * 与用户端 feedApi 的 pinPost/unpinPost（/feed/:id/pin）路径不同，勿混用。
 */

export function pinPost(postId: number) {
  return request.post<ApiResult<void>>(`/admin/feed/pin/${postId}`)
}

export function unpinPost(postId: number) {
  return request.post<ApiResult<void>>(`/admin/feed/unpin/${postId}`)
}
