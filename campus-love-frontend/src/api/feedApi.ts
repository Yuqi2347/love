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
  videos: string | null
  linkUrl: string | null
  linkTitle: string | null
  linkImage: string | null
  likeCount: number
  commentCount: number
  liked: boolean
  createdAt: string
  comments: FeedComment[]
}

export function createPost(data: {
  content: string
  images?: string
  videos?: string
  linkUrl?: string
  linkTitle?: string
  linkImage?: string
}) {
  return request.post<ApiResult<FeedPost>>('/feed', data)
}

export function createDiscoveryPost(data: {
  content: string
  images?: string
  videos?: string
  linkUrl?: string
  linkTitle?: string
  linkImage?: string
}) {
  return request.post<ApiResult<FeedPost>>('/feed/discovery', data)
}

// 上传图片 - 返回图片路径字符串
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResult<string>>('/feed/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 上传视频 - 返回视频路径字符串
export function uploadVideo(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResult<string>>('/feed/upload/video', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getTimeline(page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>('/feed/timeline', { params: { page, size } })
}

export function getUserPosts(userId: number, page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>(`/feed/user/${userId}`, { params: { page, size } })
}

export function getUserTimelinePosts(userId: number, page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>(`/feed/user/${userId}/timeline`, { params: { page, size } })
}

export function getUserDiscoveryPosts(userId: number, page = 0, size = 10) {
  return request.get<ApiResult<FeedPost[]>>(`/feed/user/${userId}/discovery`, { params: { page, size } })
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

export function getDiscoveryPosts(page = 0, size = 10, keyword?: string) {
  return request.get<ApiResult<FeedPost[]>>('/feed/discovery', { params: { page, size, keyword } })
}

export function getPostDetail(postId: number) {
  return request.get<ApiResult<FeedPost>>(`/feed/${postId}`)
}

export interface UserLevelInfo {
  score: number
  level: number
  progress: number
  scoreToNext: number
}

export function getLevelInfo() {
  return request.get<ApiResult<UserLevelInfo>>('/feed/level-info')
}
