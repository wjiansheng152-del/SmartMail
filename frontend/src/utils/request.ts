/**
 * Axios 封装：baseURL、请求头注入、统一 Result 解析、401 刷新与错误处理
 * 通过 setAuthProvider 注入 store 依赖，避免循环引用
 */
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { Result } from '@/types/api'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** 由 main.ts 注入：从 store 获取 token、tenantId 及执行刷新/登出 */
export interface AuthProvider {
  getAccessToken: () => string
  getTenantId: () => string
  refreshToken: () => Promise<void>
  logout: () => void
}

let authProvider: AuthProvider | null = null

export function setAuthProvider(provider: AuthProvider) {
  authProvider = provider
}

const requestInstance: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 请求拦截：注入 Authorization、X-Tenant-Id */
requestInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (authProvider) {
      const token = authProvider.getAccessToken()
      if (token) config.headers.Authorization = `Bearer ${token}`
      const tenantId = authProvider.getTenantId()
      if (tenantId) config.headers['X-Tenant-Id'] = tenantId
    }
    return config
  },
  (err) => Promise.reject(err)
)

/** 是否正在刷新 Token，避免并发重复刷新 */
let isRefreshing = false
/** 401 等待队列，刷新完成后统一重试 */
const refreshSubscribers: Array<(token: string) => void> = []

function subscribeRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers.length = 0
}

/** 响应拦截：解析 Result.data、401 时尝试刷新 Token */
requestInstance.interceptors.response.use(
  (response) => {
    const res = response.data as Result<unknown>
    if (res && typeof res === 'object' && 'data' in res) {
      response.data = res.data
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry && authProvider) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(requestInstance(originalRequest))
          })
        })
      }
      originalRequest._retry = true
      isRefreshing = true
      try {
        await authProvider.refreshToken()
        const newToken = authProvider.getAccessToken()
        onRefreshed(newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return requestInstance(originalRequest)
      } catch (e) {
        authProvider.logout()
        window.location.href = '/login'
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(error)
  }
)

export { requestInstance as request }
export default requestInstance
