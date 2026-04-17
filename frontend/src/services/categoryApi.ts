import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Category } from '../types/folder'

export const categoryApi = {
  getAll: async (): Promise<Category[]> => {
    const res = await apiClient.get<ApiResponse<Category[]>>('/categories')
    return res.data.data
  },
  create: async (name: string): Promise<Category> => {
    const res = await apiClient.post<ApiResponse<Category>>('/categories', { name })
    return res.data.data
  },
  update: async (id: number, name: string): Promise<Category> => {
    const res = await apiClient.put<ApiResponse<Category>>(`/categories/${id}`, { name })
    return res.data.data
  },
}
