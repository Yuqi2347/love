// 统一 Emoji 使用规范
// 功能入口 / 空状态 / 卡片标题 / 邀约类型等处使用

export const EMOJI = {
  // 邀约类型
  INVITE_DINING: '🍽️',
  INVITE_SPORT: '⚽',
  INVITE_STUDY: '📖',
  INVITE_GAME: '🎬',
  INVITE_TRAVEL: '🏕️',
  INVITE_OTHER: '🎉',

  // 功能模块
  MATCH: '🎯',
  MOMENT: '💕',
  INVITE: '📅',
  CHAT: '💬',
  PROFILE: '👤',
  FEED: '📝',

  // 状态
  EMPTY: '🫥',
  SUCCESS: '🎉',
  WARNING: '⚠️',
  LOADING: '⏳',
  HEART: '❤️',
  FIRE: '🔥',
  STAR: '⭐',
  LOCK: '🔒',

  // 性别
  MALE: '👨',
  FEMALE: '👩',

  // 学校 / 教育
  SCHOOL: '🎓',
  BOOK: '📚',

  // 社交
  WAVE: '👋',
  HANDSHAKE: '🤝',
  SPARKLE: '✨',

  // MBTI 分组
  MBTI_ANALYST: '🧠',
  MBTI_DIPLOMAT: '🌿',
  MBTI_SENTINEL: '🛡️',
  MBTI_EXPLORER: '🧭',
} as const

// 邀约类型 → emoji 映射
export const INVITE_TYPE_EMOJI: Record<string, string> = {
  DINING: EMOJI.INVITE_DINING,
  SPORT: EMOJI.INVITE_SPORT,
  STUDY: EMOJI.INVITE_STUDY,
  GAME: EMOJI.INVITE_GAME,
  TRAVEL: EMOJI.INVITE_TRAVEL,
  OTHER: EMOJI.INVITE_OTHER,
}

// MBTI 分组色彩
export const MBTI_GROUPS = {
  analyst: {
    emoji: EMOJI.MBTI_ANALYST,
    label: '分析师',
    color: '#8B5CF6',
    bg: 'rgba(139, 92, 246, 0.08)',
    types: ['INTJ', 'INTP', 'ENTJ', 'ENTP'],
  },
  diplomat: {
    emoji: EMOJI.MBTI_DIPLOMAT,
    label: '外交家',
    color: '#10B981',
    bg: 'rgba(16, 185, 129, 0.08)',
    types: ['INFJ', 'INFP', 'ENFJ', 'ENFP'],
  },
  sentinel: {
    emoji: EMOJI.MBTI_SENTINEL,
    label: '守卫者',
    color: '#3B82F6',
    bg: 'rgba(59, 130, 246, 0.08)',
    types: ['ISTJ', 'ISFJ', 'ESTJ', 'ESFJ'],
  },
  explorer: {
    emoji: EMOJI.MBTI_EXPLORER,
    label: '探险家',
    color: '#F59E0B',
    bg: 'rgba(245, 158, 11, 0.08)',
    types: ['ISTP', 'ISFP', 'ESTP', 'ESFP'],
  },
} as const

export function getMbtiGroup(mbti: string) {
  for (const [key, group] of Object.entries(MBTI_GROUPS)) {
    if (group.types.includes(mbti)) return { key, ...group }
  }
  return null
}
