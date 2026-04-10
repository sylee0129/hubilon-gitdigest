import apiClient from './axios'

interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: {
    id: number
    name: string
    email: string
    department: string | null
    role: 'ADMIN' | 'USER'
  }
}

interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

const authApi = {
  login: async (email: string, password: string): Promise<LoginResponse> => {
    const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', { email, password })
    return res.data.data
  },
  refresh: async (): Promise<string> => {
    const res = await apiClient.post<ApiResponse<{ accessToken: string; expiresIn: number }>>('/auth/refresh', {})
    return res.data.data.accessToken
  },
  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post('/auth/logout', { refreshToken })
  },
  me: async () => {
    const res = await apiClient.get<ApiResponse<{ id: number; name: string; email: string; department: string | null; role: 'ADMIN' | 'USER' }>>('/auth/me')
    return res.data.data
  },
}

export default authApi
