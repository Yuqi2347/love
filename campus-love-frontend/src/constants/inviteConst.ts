// 邀约类型
export enum InviteType {
  DINNER = 'DINNER',
  SPORT = 'SPORT',
  STUDY = 'STUDY',
  DRAMA = 'DRAMA',
  OTHER = 'OTHER',
}

export const INVITE_TYPE_LABELS: Record<InviteType, string> = {
  [InviteType.DINNER]: '约饭',
  [InviteType.SPORT]: '运动',
  [InviteType.STUDY]: '自习',
  [InviteType.DRAMA]: '追剧',
  [InviteType.OTHER]: '其他',
}

export const INVITE_TYPE_OPTIONS = [
  { label: INVITE_TYPE_LABELS[InviteType.DINNER], value: InviteType.DINNER, icon: '🍽️' },
  { label: INVITE_TYPE_LABELS[InviteType.SPORT], value: InviteType.SPORT, icon: '🏀' },
  { label: INVITE_TYPE_LABELS[InviteType.STUDY], value: InviteType.STUDY, icon: '📚' },
  { label: INVITE_TYPE_LABELS[InviteType.DRAMA], value: InviteType.DRAMA, icon: '🎬' },
  { label: INVITE_TYPE_LABELS[InviteType.OTHER], value: InviteType.OTHER, icon: '🎯' },
]

// 邀约模板（用于一键填充）
export interface InviteTemplate {
  label: string
  type: InviteType
  title: string
  content: string
  atmosphereTags: string[]
}

export const INVITE_TEMPLATES: InviteTemplate[] = [
  {
    label: '食堂拼饭',
    type: InviteType.DINNER,
    title: '一起去食堂拼个晚饭',
    content: '今晚想去食堂试试新窗口，一个人吃有点无聊，一起拼个桌聊聊天～',
    atmosphereTags: ['SOCIAL', 'RELAXED'],
  },
  {
    label: '图书馆自习',
    type: InviteType.STUDY,
    title: '图书馆安静自习',
    content: '备考周，一起去图书馆自习，互相监督不玩手机，有需要可以顺便问问题。',
    atmosphereTags: ['QUIET', 'SERIOUS'],
  },
  {
    label: '操场夜跑',
    type: InviteType.SPORT,
    title: '操场慢跑/散步',
    content: '晚上去操场慢跑散步，节奏轻松，想聊就聊，不想聊就听歌放空。',
    atmosphereTags: ['RELAXED', 'ACTIVE'],
  },
]

// 邀约模式
export enum InviteMode {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

export const INVITE_MODE_LABELS: Record<InviteMode, string> = {
  [InviteMode.PUBLIC]: '公开邀约',
  [InviteMode.PRIVATE]: '一对一邀约',
}

// 邀约状态
export enum InviteStatus {
  RECRUITING = 'RECRUITING',
  FULL = 'FULL',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  ENDED = 'ENDED',
  CANCELLED = 'CANCELLED',
}

export const INVITE_STATUS_LABELS: Record<InviteStatus, string> = {
  [InviteStatus.RECRUITING]: '招募中',
  [InviteStatus.FULL]: '已满员',
  [InviteStatus.CONFIRMED]: '已确认',
  [InviteStatus.IN_PROGRESS]: '进行中',
  [InviteStatus.ENDED]: '已结束',
  [InviteStatus.CANCELLED]: '已取消',
}

export const INVITE_STATUS_COLORS: Record<InviteStatus, string> = {
  [InviteStatus.RECRUITING]: '#52c41a', // 绿色
  [InviteStatus.FULL]: '#faad14', // 橙色
  [InviteStatus.CONFIRMED]: '#1890ff', // 蓝色
  [InviteStatus.IN_PROGRESS]: '#722ed1', // 紫色
  [InviteStatus.ENDED]: '#8c8c8c', // 灰色
  [InviteStatus.CANCELLED]: '#ff4d4f', // 红色
}

// 邀约周期
export enum InvitePeriod {
  ONCE = 'ONCE',
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
}

export const INVITE_PERIOD_LABELS: Record<InvitePeriod, string> = {
  [InvitePeriod.ONCE]: '单次',
  [InvitePeriod.WEEKLY]: '每周',
  [InvitePeriod.MONTHLY]: '每月',
}

export const INVITE_PERIOD_OPTIONS = [
  { label: INVITE_PERIOD_LABELS[InvitePeriod.ONCE], value: InvitePeriod.ONCE },
  { label: INVITE_PERIOD_LABELS[InvitePeriod.WEEKLY], value: InvitePeriod.WEEKLY },
  { label: INVITE_PERIOD_LABELS[InvitePeriod.MONTHLY], value: InvitePeriod.MONTHLY },
]

// 氛围标签
export interface AtmosphereTag {
  label: string
  value: string
  color: string
}

export const ATMOSPHERE_TAGS: AtmosphereTag[] = [
  { label: '社牛友好', value: 'SOCIAL', color: '#ff6b9d' },
  { label: '适合i人', value: 'INTROVERT', color: '#a855f7' },
  { label: '轻松愉快', value: 'RELAXED', color: '#22c55e' },
  { label: '严肃认真', value: 'SERIOUS', color: '#3b82f6' },
  { label: '安静专注', value: 'QUIET', color: '#6366f1' },
  { label: '活跃互动', value: 'ACTIVE', color: '#f97316' },
]

// 等待邀约有效期
export const WAIT_DURATION_OPTIONS = [
  { label: '1小时', value: 1 },
  { label: '3小时', value: 3 },
  { label: '6小时', value: 6 },
  { label: '12小时', value: 12 },
  { label: '24小时', value: 24 },
]

// 默认截止时间
export const DEFAULT_DEADLINE_HOURS = 1

// 格式化邀约时间
export function formatInviteTime(timeStr: string): string {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = date.getTime() - now.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (diff < 0) return '已过期'
  if (hours < 1) return '即将开始'
  if (hours < 24) return `${hours}小时后`
  if (days < 7) return `${days}天后`
  return date.toLocaleDateString('zh-CN')
}

// 格式化邀约周期配置
export function formatPeriodConfig(config: string): string {
  if (!config) return ''
  try {
    const parsed = JSON.parse(config)
    if (parsed.weekday !== undefined) {
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return `${weekdays[parsed.weekday]} ${parsed.hour}:00`
    }
    if (parsed.day !== undefined) {
      return `每月${parsed.day}号 ${parsed.hour}:00`
    }
    return config
  } catch {
    return config
  }
}
