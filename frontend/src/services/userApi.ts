import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { User } from '../types/folder'

export const userApi = {
  search: async (q?: string): Promise<User[]> => {
    const res = await apiClient.get<ApiResponse<User[]>>('/users', { params: q ? { q } : undefined })
    return res.data.data
  },
  create: async (payload: { name: string; email: string; password: string; teamId?: number }): Promise<User> => {
    const res = await apiClient.post<ApiResponse<User>>('/users', payload)
    return res.data.data
  },
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/users/${id}`)
  },
}
