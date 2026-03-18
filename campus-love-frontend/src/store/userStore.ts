import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserProfile } from '@/api/userApi'
import { getMyProfile } from '@/api/userApi'
import type { AuthResponse } from '@/api/authApi'
import { useFollowStore } from './followStore'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserProfile | null>(null)
  const isLoggedIn = ref(!!localStorage.getItem('access_token'))

  async function setAuth(auth: AuthResponse) {
    localStorage.setItem('access_token', auth.accessToken)
    localStorage.setItem('refresh_token', auth.refreshToken)
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
  }

  async function fetchProfile() {
    try {
      const res = await getMyProfile()
      user.value = res.data.data
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
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    isLoggedIn.value = false
    user.value = null
    useFollowStore().clear()
  }

  return { user, isLoggedIn, setAuth, fetchProfile, logout }
})
