import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/useAuthStore'

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// refresh 전용 인스턴스 — apiClient 순환 참조 방지
const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true
        try {
          const refreshToken = useAuthStore.getState().refreshToken
          const res = await refreshClient.post<{
            success: boolean
            data: { accessToken: string; expiresIn: number }
            message: string | null
          }>('/auth/refresh', { refreshToken })
          const newToken = res.data.data.accessToken
          useAuthStore.getState().setAccessToken(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return apiClient(originalRequest)
        } catch {
          useAuthStore.getState().clearAuth()
          window.location.href = '/login'
          return Promise.reject(error)
        }
      }

      const message =
        (error.response?.data as { message?: string })?.message ??
        error.message ??
        '요청 처리 중 오류가 발생했습니다.'
      return Promise.reject(new Error(message))
    }

    return Promise.reject(error)
  },
)

export default apiClient
