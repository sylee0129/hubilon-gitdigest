import apiClient from './axios'
import type { ApiResponse } from '../types/api'

export interface Team {
  id: number
  name: string
}

export const teamApi = {
  getTeams: async (): Promise<Team[]> => {
    const res = await apiClient.get<ApiResponse<Team[]>>('/teams')
    return res.data.data ?? []
  },
}
