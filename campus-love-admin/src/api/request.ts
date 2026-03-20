import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('admin_token')
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
        localStorage.removeItem('admin_token')
        window.location.href = '/admin/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (status === 401 || status === 403) {
      localStorage.removeItem('admin_token')
      ElMessage.error(status === 401 ? '登录已过期，请重新使用管理员账号登录' : '无权限访问，请使用管理员账号登录')
      window.location.href = '/admin/login'
      return Promise.reject(error)
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default service
