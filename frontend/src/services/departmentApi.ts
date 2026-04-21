import apiClient from './axios'
import type { ApiResponse } from '../types/api'

export interface TeamItem {
  id: number
  name: string
}

export interface Department {
  id: number
  name: string
  teams: TeamItem[]
}

export const departmentApi = {
  getAll: async (): Promise<Department[]> => {
    const res = await apiClient.get<ApiResponse<Department[]>>('/departments')
    return res.data.data ?? []
  },
}
