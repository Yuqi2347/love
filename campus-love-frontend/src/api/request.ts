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
  timeout: 30000, // 外网/隧道访问延迟较高，适当放宽
})

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
      if (res.code === 401) {
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    let msg = error.response?.data?.message || error.message || '网络错误'
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      msg = '请求超时，请检查网络或稍后重试（外网访问可能较慢）'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default service
