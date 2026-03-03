export enum FollowStatus {
  NONE = 'NONE',
  ONE_WAY = 'ONE_WAY',
  MUTUAL = 'MUTUAL',
}

export const FOLLOW_STATUS_LABELS: Record<FollowStatus, string> = {
  [FollowStatus.NONE]: '关注',
  [FollowStatus.ONE_WAY]: '已关注',
  [FollowStatus.MUTUAL]: '互相关注',
}

export const DAILY_CHAT_LIMIT = 5
export const DAILY_INVITE_LIMIT = 3
