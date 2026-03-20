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
  timeout: 60000,
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
      return Promise.reject(error)
    }
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearAuthAndRedirectToLogin()
      return Promise.reject(error)
    }
    if (error.response?.status === 413) {
      const backendMsg = error.response?.data?.message
      ElMessage.warning(backendMsg || '文件过大，请压缩后重试')
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default service
