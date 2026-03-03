import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserProfile } from '@/api/userApi'
import { getMyProfile } from '@/api/userApi'
import type { AuthResponse } from '@/api/authApi'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserProfile | null>(null)
  const isLoggedIn = ref(!!localStorage.getItem('access_token'))

  function setAuth(auth: AuthResponse) {
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
  }

  async function fetchProfile() {
    try {
      const res = await getMyProfile()
      user.value = res.data.data
    } catch {
      logout()
    }
  }

  function logout() {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    isLoggedIn.value = false
    user.value = null
  }

  return { user, isLoggedIn, setAuth, fetchProfile, logout }
})
