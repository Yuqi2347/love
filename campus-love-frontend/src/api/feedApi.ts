import request from './request'
import type { ApiResult } from './request'

export interface FeedComment {
  id: number
  userId: number
  nickname: string
  avatarUrl: string | null
  content: string
  images?: string | null
  parentId: number | null
  repliedToName?: string | null  // 被回复的用户昵称
  repliedUserId?: number | null  // 被回复的用户ID
  createdAt: string
  deleted?: boolean
  /** V39：点赞数 */
  likeCount?: number
  /** V39：当前用户是否已点赞 */
  liked?: boolean
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
  visibility?: string
  createdAt: string
  comments: FeedComment[]
  /** V24：AI 提取的标签，逗号分隔 */
  aiTags?: string | null
  /** V39：是否置顶 */
  pinned?: boolean
  /** V39：置顶时间 */
  pinnedAt?: string | null
}

export function createPost(data: {
  content: string
  images?: string
  videos?: string
  linkUrl?: string
  linkTitle?: string
  linkImage?: string
  visibility?: string
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
  visibility?: string
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

export function getTimeline(page = 0, size = 10, sort?: string) {
  return request.get<ApiResult<FeedPost[]>>('/feed/timeline', { params: { page, size, sort } })
}

export function getUserPostsSummary(userId: number) {
  return request.get<ApiResult<{ total: number; recentImageUrls: string[] }>>(`/feed/user/${userId}/summary`)
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

export function likeComment(commentId: number) {
  return request.post<ApiResult<void>>(`/feed/like/comment/${commentId}`)
}

export function unlikeComment(commentId: number) {
  return request.delete<ApiResult<void>>(`/feed/like/comment/${commentId}`)
}

export function addComment(data: { postId: number; content: string; images?: string; parentId?: number; repliedUserId?: number }) {
  return request.post<ApiResult<void>>('/feed/comment', data)
}

export function deleteComment(commentId: number) {
  return request.delete<ApiResult<void>>(`/feed/comment/${commentId}`)
}

export function deletePost(postId: number) {
  return request.delete<ApiResult<void>>(`/feed/${postId}`)
}

export function updatePostAiTags(postId: number, aiTags: string) {
  return request.patch<ApiResult<void>>(`/feed/${postId}/ai-tags`, null, { params: { aiTags } })
}

export function retagPost(postId: number) {
  return request.post<ApiResult<void>>(`/feed/retag/${postId}`)
}

export function getDiscoveryPosts(page = 0, size = 10, sort?: string, keyword?: string) {
  return request.get<ApiResult<FeedPost[]>>('/feed/discovery', { params: { page, size, sort, keyword } })
}

export function getPostDetail(postId: number, commentSort?: string) {
  const id = Number(postId)
  if (!Number.isFinite(id) || id <= 0) {
    return Promise.reject(new Error('无效的动态 ID'))
  }
  return request.get<ApiResult<FeedPost>>(`/feed/${id}`, { params: commentSort ? { commentSort } : {} })
}

export function pinPost(postId: number) {
  return request.post<ApiResult<void>>(`/feed/${postId}/pin`)
}

export function unpinPost(postId: number) {
  return request.delete<ApiResult<void>>(`/feed/${postId}/pin`)
}

export function getLikedPosts(page = 0, size = 20) {
  return request.get<ApiResult<FeedPost[]>>('/feed/liked-posts', { params: { page, size } })
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

export function sharePost(postId: number, receiverIds: number[]) {
  return request.post<ApiResult<void>>('/feed/share', { postId, receiverIds })
}
