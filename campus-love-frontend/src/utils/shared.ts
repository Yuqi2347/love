// === 共享工具函数 ===

import { useUserStore } from '@/store/userStore'

const AVATAR_BG_COLORS = [
  '#4A6CF7',
  '#0EA5A4',
  '#F59E0B',
  '#EC4899',
  '#22C55E',
  '#F97316',
  '#3B82F6',
  '#8B5CF6',
]

function pickInitial(name?: string | null): string {
  const text = (name ?? '').trim()
  if (!text) return '友'
  return text.charAt(0).toUpperCase()
}

function hashText(input: string): number {
  let hash = 0
  for (let i = 0; i < input.length; i += 1) {
    hash = ((hash << 5) - hash) + input.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

function pickAvatarColor(name?: string | null): string {
  const seed = (name ?? '').trim() || 'default'
  return AVATAR_BG_COLORS[hashText(seed) % AVATAR_BG_COLORS.length] || '#4A6CF7'
}

/** 根据昵称生成默认头像（圆形底 + 首字） */
export function buildDefaultAvatar(name?: string | null): string {
  const initial = pickInitial(name)
  const bg = pickAvatarColor(name)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="${bg}" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="#ffffff" font-size="42" font-family="PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif">${initial}</text></svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

// 默认头像 SVG（无昵称场景兜底）
export const DEFAULT_AVATAR = buildDefaultAvatar()

// 媒体 URL 处理：相对上传路径补全为 /api 前缀（与 Spring context-path=/api 一致）；blob/data 原样返回
export function getMediaUrl(url: string | null): string {
  if (!url) return ''
  const u = url.trim()
  if (!u) return ''
  if (u.startsWith('blob:') || u.startsWith('data:')) return u
  let out: string
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('/api')) {
    out = u
  } else {
    out = '/api' + (u.startsWith('/') ? u : '/' + u)
  }
  const avatarPath = out.match(/\/user\/avatar\/(\d+)/)
  const avatarUid = avatarPath ? Number(avatarPath[1]) : null
  if (avatarUid != null) {
    try {
      const store = useUserStore()
      const me = store.user?.id
      const n = store.avatarDisplayNonce
      // 仅对「当前登录用户」的头像拼 cache-bust，避免换头像时整页所有头像强制重载
      if (me != null && avatarUid === me && n) {
        out += (out.includes('?') ? '&' : '?') + '_av=' + n
      }
    } catch {
      /* Pinia 未就绪时忽略 */
    }
  }
  return out
}

const UPLOADS_PREFIX = '/uploads/'

/**
 * 与后端 FeedImageThumbPaths 一致：动态 feed 图在同目录下的 thumb_ 缩略图 URL。
 * 无法推导时返回原路径（老数据或非 feed_img 命名）。
 */
export function feedImageThumbPathOrSelf(imagePath: string): string {
  const p = imagePath.trim()
  if (!p.startsWith(UPLOADS_PREFIX)) return p
  const rest = p.slice(UPLOADS_PREFIX.length)
  const slash = rest.indexOf('/')
  let dirPrefix = ''
  let filename: string
  if (slash < 0) {
    filename = rest
  } else {
    const seg = rest.slice(0, slash)
    if (!/^\d+$/.test(seg)) return p
    dirPrefix = `${seg}/`
    filename = rest.slice(slash + 1)
  }
  if (!filename.startsWith('feed_img_')) return p
  const dot = filename.lastIndexOf('.')
  if (dot <= 0) return p
  const ext = filename.slice(dot).toLowerCase()
  if (!['.jpg', '.jpeg', '.png', '.webp', '.bmp'].includes(ext)) return p
  const base = filename.slice(0, dot)
  return `${UPLOADS_PREFIX}${dirPrefix}thumb_${base}.jpg`
}

/** 列表卡片用图：与后端 imageThumbs 同序；缺省时按路径推导 thumb_（评论图等无 imageThumbs 字段时也可用） */
export function feedCardImagePaths(post: { images?: string | null; imageThumbs?: string | null }): string[] {
  const full = (post.images ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  const thumbs = (post.imageThumbs ?? '')
    .split(',')
    .map((s) => s.trim())
  return full.map((u, i) => {
    const th = thumbs[i]?.trim()
    if (th) return th
    return feedImageThumbPathOrSelf(u)
  })
}

// 邀约类型颜色映射
const TYPE_COLORS: Record<string, string> = {
  DINNER: '#ff6b9d',
  SPORT: '#52c41a',
  STUDY: '#1890ff',
  DRAMA: '#722ed1',
  OTHER: '#8c8c8c',
}

export function getTypeColor(type: string): string {
  return TYPE_COLORS[type] || '#8c8c8c'
}

// 相对时间格式化（"刚刚" / "3分钟前" / "2小时前" / "5天前"）
export function formatRelativeTime(timeStr: string): string {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - d.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return d.toLocaleDateString('zh-CN')
}
