import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, type AuthResponse } from '@/api/authApi'

export const useUserStore = defineStore('adminUser', () => {
  const token = ref<string | null>(localStorage.getItem('admin_token'))
  const user = ref<Pick<AuthResponse, 'userId' | 'email' | 'nickname' | 'isAdmin'> | null>(null)

  async function login(email: string, password: string) {
    const res = await apiLogin({ email, password })
    const data = res.data.data
    const isAdmin = data && (data.isAdmin === true || (data as { admin?: boolean }).admin === true)
    if (!isAdmin) {
      throw new Error('该账号不是管理员，无法登录后台。请确认已在 campus_love 库中执行 set_admin.sql 并将 is_admin=1 赋给该邮箱。')
    }
    token.value = data!.accessToken
    user.value = {
      userId: data!.userId,
      email: data!.email,
      nickname: data!.nickname,
      isAdmin: true,
    }
    localStorage.setItem('admin_token', data!.accessToken)
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('admin_token')
  }

  const isLoggedIn = ref(!!token.value)

  return { token, user, isLoggedIn, login, logout }
})
