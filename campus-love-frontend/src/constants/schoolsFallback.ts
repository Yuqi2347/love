import type { SchoolItem } from '@/api/authApi'

/**
 * 与后端 `schools.json` 保持同步的本地副本。
 * 当 /api/auth/schools 因网络抖动（如 ERR_CONNECTION_CLOSED）失败时用于注册页降级，避免「无数据」。
 */
export const SCHOOLS_FALLBACK: SchoolItem[] = [
  {
    name: '深圳大学',
    domain: 'mails.szu.edu.cn',
    emailSuffix: 'szu.edu.cn',
    emailDomains: ['mails.szu.edu.cn', 'email.szu.edu.cn'],
    campuses: ['沧海校区', '粤海校区', '丽湖校区'],
  },
]

export function filterSchoolsByKeyword(keyword: string): SchoolItem[] {
  const q = keyword.trim()
  if (q.length < 2) return []
  return SCHOOLS_FALLBACK.filter(
    (s) =>
      s.name.includes(q) ||
      (s.domain && s.domain.toLowerCase().includes(q.toLowerCase())) ||
      (s.emailSuffix && s.emailSuffix.toLowerCase().includes(q.toLowerCase())),
  )
}
