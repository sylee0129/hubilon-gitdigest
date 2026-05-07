import apiClient from './axios'
import type { ApiResponse } from '../types/api'

interface UserInfo {
  id: number
  name: string
  email: string
  teamId: number | null
  teamName: string | null
  role: 'ADMIN' | 'USER'
}

const authApi = {
  me: async (): Promise<UserInfo> => {
    const res = await apiClient.get<ApiResponse<UserInfo>>('/auth/me')
    return res.data.data
  },
}

export default authApi
