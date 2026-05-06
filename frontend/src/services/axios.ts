import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import keycloak from '../lib/keycloak'

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  if (keycloak.isTokenExpired(30)) {
    await keycloak.updateToken(30)
  }
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
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
