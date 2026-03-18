// === 共享工具函数 ===

// 默认头像 SVG（可缩放，不依赖固定尺寸）
export const DEFAULT_AVATAR = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'

// 媒体 URL 处理（补全 /api 前缀）
export function getMediaUrl(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
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
