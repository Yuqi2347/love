import request from './request'
import type { ApiResult } from './request'

export interface FeedComment {
  id: number
  userId: number
  nickname: string
  avatarUrl: string | null
  content: string
  parentId: number | null
  createdAt: string
}

export interface FeedPost {
  id: number
  userId: number
  nickname: string
  avatarUrl: string | null
  content: string
  images: string | null
  likeCount: number
  commentCount: number
  liked: boolean
  createdAt: string
  comments: FeedComment[]
}

export function createPost(data: { content: string; images?: string }) {
  return request.post<ApiResult<FeedPost>>('/feed', data)
}

export function getTimeline(page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>('/feed/timeline', { params: { page, size } })
}

export function getUserPosts(userId: number, page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>(`/feed/user/${userId}`, { params: { page, size } })
}

export function likePost(postId: number) {
  return request.post<ApiResult<void>>(`/feed/like/${postId}`)
}

export function unlikePost(postId: number) {
  return request.delete<ApiResult<void>>(`/feed/like/${postId}`)
}

export function addComment(data: { postId: number; content: string; parentId?: number }) {
  return request.post<ApiResult<void>>('/feed/comment', data)
}

export function deletePost(postId: number) {
  return request.delete<ApiResult<void>>(`/feed/${postId}`)
}
