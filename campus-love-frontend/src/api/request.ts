import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000, // AI 接口耗时较长，放宽到 60s
})

function clearAuthAndRedirectToLogin() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  window.location.href = '/login'
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401 || res.code === 403) {
        clearAuthAndRedirectToLogin()
      }
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    const isNetworkError = !error.response && (error.code === 'ERR_NETWORK' || error.message?.includes('Network Error'))
    const isTimeout = error.code === 'ECONNABORTED' || error.message?.includes('timeout')
    if (isTimeout) {
      ElMessage.error('请求超时，请检查网络或稍后重试')
      return Promise.reject(error)
    }
    if (isNetworkError) {
      const method = (error.config?.method || 'GET').toUpperCase()
      const url = error.config?.url || '(unknown)'
      // 网络抖动不应直接清空登录态，保留 token 便于用户自动恢复
      console.warn(`[api] network error: ${method} ${url}`, error)
      ElMessage.error('网络连接异常，请稍后重试')
      return Promise.reject(error)
    }
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearAuthAndRedirectToLogin()
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default service
