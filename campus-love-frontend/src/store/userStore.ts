import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserProfile } from '@/api/userApi'
import { getMyProfile } from '@/api/userApi'
import type { AuthResponse } from '@/api/authApi'
import { useFollowStore } from './followStore'
import { useChatStore } from './chatStore'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserProfile | null>(null)
  const isLoggedIn = ref(!!localStorage.getItem('access_token'))
  /** 递增后拼到 /user/avatar/ 请求上，强制本端换图（与后端 no-store 配合） */
  const avatarDisplayNonce = ref(0)

  function bumpAvatarImageCache() {
    avatarDisplayNonce.value = Date.now()
  }

  async function setAuth(auth: AuthResponse) {
    const chatStore = useChatStore()
    chatStore.disconnect()
    localStorage.setItem('access_token', auth.accessToken)
    localStorage.setItem('refresh_token', auth.refreshToken)
    localStorage.setItem('userId', String(auth.userId))
    isLoggedIn.value = true
    user.value = {
      id: auth.userId,
      email: auth.email,
      nickname: auth.nickname,
      avatarUrl: auth.avatarUrl,
      profileComplete: auth.profileComplete,
    } as UserProfile
    // 立即获取完整用户信息（包含isAdmin和userLevel）
    await fetchProfile()
    chatStore.connectWebSocket()
  }

  async function fetchProfile() {
    try {
      const res = await getMyProfile()
      const prevAt = user.value?.avatarUpdatedAt
      user.value = res.data.data
      const nextAt = user.value?.avatarUpdatedAt
      if (nextAt != null && nextAt !== prevAt) {
        bumpAvatarImageCache()
      }
      if (user.value?.id != null) {
        localStorage.setItem('userId', String(user.value.id))
      }
    } catch (error: any) {
      const status = error?.response?.status
      if (status === 401 || status === 403) {
        logout()
        return
      }
      // 非鉴权类错误（如网络抖动）不主动登出，避免用户被误踢回登录页
      console.warn('[userStore] fetchProfile failed without auth error, keep login state', error)
    }
  }

  function logout() {
    useChatStore().disconnect()
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('userId')
    isLoggedIn.value = false
    user.value = null
    useFollowStore().clear()
  }

  return { user, isLoggedIn, avatarDisplayNonce, setAuth, fetchProfile, logout, bumpAvatarImageCache }
})
