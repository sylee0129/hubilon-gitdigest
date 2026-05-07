import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

let isRefreshing = false
let refreshQueue: Array<() => void> = []

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (axios.isAxiosError(error)) {
      if (
        error.response?.status === 401 &&
        !originalRequest._retry &&
        !originalRequest.url?.includes('/auth/refresh')
      ) {
        originalRequest._retry = true

        if (isRefreshing) {
          return new Promise((resolve) => {
            refreshQueue.push(() => resolve(apiClient(originalRequest)))
          })
        }

        isRefreshing = true
        try {
          await apiClient.post('/auth/refresh')
          refreshQueue.forEach((cb) => cb())
          refreshQueue = []
          return apiClient(originalRequest)
        } catch {
          window.location.href = '/auth/login'
          return Promise.reject(error)
        } finally {
          isRefreshing = false
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
