import request from './request'
import type { ApiResult } from './request'

export interface UserProfile {
  id: number
  email: string
  nickname: string
  gender: number
  birthDate: string | null
  birthTime: string | null
  /** 年龄（仅他人资料返回，本人为 undefined） */
  age?: number | null
  school: string | null
  major: string | null
  grade: string | null
  activityScore: number
  userLevel: number
  isAdmin: boolean
  creditScore: number
  inviteCount: number
  participateCount: number
  inviteConcurrentLimit?: number
  invitePublicConcurrentLimit?: number
  invitePrivateConcurrentLimit?: number
  inviteDailyLimit?: number
  mbti: string | null
  zodiac: string | null
  bazi: string | null
  baziUnknown?: boolean
  avatarUrl: string | null
  /** 头像更新时间戳（毫秒），固定路径换图时供前端失效缓存 */
  avatarUpdatedAt?: number | null
  bio: string | null
  /** 旧格式（兼容），新格式用 interestTags */
  interests?: string | null
  /** 新格式：{ dimension: [{code, sharing, intensity}] } */
  interestTags?: Record<string, { code: string; sharing: number; intensity: number }[]> | null
  profileComplete: boolean
  /** 朋友圈可见性：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
  feedVisibility?: string
  /** 动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部 */
  feedVisibilityTime?: number
  /** 个人主页背景图 URL */
  coverImageUrl?: string | null
  /** V24：是否开启破冰功能 */
  iceBreakEnabled?: boolean | null
  /** V24：AI 信息公开授权设置 JSON */
  aiDisclosureSettings?: string | null
  /** 关注数（资料接口返回，他人主页不可拉名单） */
  followingCount?: number
  followerCount?: number
  mutualCount?: number
}

export interface UpdateProfileParams {
  nickname: string
  gender: number
  birthDate: string
  birthTime?: string
  /** 不知道时辰时八字权重清零 */
  baziUnknown?: boolean
  school?: string
  major?: string
  grade?: string
  mbti?: string
  bio?: string
  /** 旧格式（兼容） */
  interests?: string
  /** 新格式 JSON 字符串：{"dimension":[{"code":"tag_xxx","sharing":0.5,"intensity":0.5}]} */
  interestTags?: string
  /** 朋友圈可见性：ALL=所有人可见，FOLLOWERS=粉丝可见，SELF=仅自己可见 */
  feedVisibility?: string
}

export function getMyProfile() {
  return request.get<ApiResult<UserProfile>>('/user/me')
}

export interface UserAiProfile {
  userId: number
  hasRealOcean: boolean
  oceanO: number | null
  oceanC: number | null
  oceanE: number | null
  oceanA: number | null
  oceanN: number | null
  naturalLanguageTags: string[]
}

export function getMyAiProfile() {
  return request.get<ApiResult<UserAiProfile>>('/user/ai-profile')
}

export function getUserProfile(userId: number) {
  return request.get<ApiResult<UserProfile>>(`/user/${userId}`)
}

export interface UserSearchItem {
  id: number
  nickname: string
  avatarUrl: string | null
}

export function searchUsers(keyword: string, limit = 10) {
  return request.get<ApiResult<UserSearchItem[]>>('/user/search', {
    params: { keyword: keyword.trim(), limit },
  })
}

export function updateProfile(data: UpdateProfileParams) {
  return request.put<ApiResult<UserProfile>>('/user/profile', data)
}

export function updateNickname(nickname: string) {
  return request.patch<ApiResult<UserProfile>>('/user/nickname', null, { params: { nickname } })
}

export function updateFeedVisibility(visibility: string) {
  return request.patch<ApiResult<UserProfile>>('/user/feed-visibility', null, { params: { visibility } })
}

export function updateFeedVisibilityTime(days: number) {
  return request.patch<ApiResult<UserProfile>>('/user/feed-visibility-time', null, { params: { days } })
}

export function updateIceBreakEnabled(enabled: boolean) {
  return request.patch<ApiResult<UserProfile>>('/user/ice-break', null, { params: { enabled } })
}

/** 破冰功能状态（用于聊天框「💡 破冰灵感」按钮及「允许对方使用破冰」开关） */
export interface IceBreakStatus {
  canShow: boolean
  targetEnabled: boolean
  allowedByMe: boolean
  canAllow: boolean
}

export function getIceBreakStatus(targetUserId: number) {
  return request.get<ApiResult<IceBreakStatus>>(`/chat/ice-break-status/${targetUserId}`)
}

/** 破冰灵感：先分析聊天记录，再给话题建议（需互关且对方已允许） */
export interface IceBreakTopicsResponse {
  analysis: string
  topics: string[]
}
export function getIceBreakTopics(targetUserId: number) {
  return request.get<ApiResult<IceBreakTopicsResponse>>(`/chat/ice-break-topics/${targetUserId}`)
}

/** 按好友单独设置：允许/禁止对方获取破冰灵感（需互关） */
export function updateIceBreakAllow(targetUserId: number, allowed: boolean) {
  return request.patch<ApiResult<void>>(`/chat/ice-break-allow/${targetUserId}`, null, { params: { allowed } })
}

export interface AiDisclosureSettings {
  mbti?: boolean
  zodiac?: boolean
  majorCategory?: boolean
  interestTags?: boolean
  naturalLangTags?: boolean
  baziInfo?: boolean
  questionnaireHints?: boolean
}

export function updateAiDisclosureSettings(settings: AiDisclosureSettings) {
  return request.patch<ApiResult<UserProfile>>('/user/ai-disclosure', { settings } as { settings: Record<string, boolean> })
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<ApiResult<string>>('/user/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function uploadCover(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.put<ApiResult<string>>('/user/cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function clearCover() {
  return request.delete<ApiResult<void>>('/user/cover')
}

// 发送密码修改验证码
export function sendPasswordCode() {
  return request.post<ApiResult<void>>('/user/password/send-code')
}

// 通过验证码修改密码
export function resetPassword(code: string, newPassword: string) {
  return request.post<ApiResult<void>>('/user/password/reset', null, {
    params: { code, newPassword }
  })
}
