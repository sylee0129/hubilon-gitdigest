import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Project, CreateProjectRequest } from '../types/report'

export const projectApi = {
  getAll: async (): Promise<Project[]> => {
    const res = await apiClient.get<ApiResponse<Project[]>>('/projects')
    return res.data.data
  },

  create: async (payload: CreateProjectRequest): Promise<Project> => {
    const res = await apiClient.post<ApiResponse<Project>>('/projects', payload)
    return res.data.data
  },

  remove: async (id: number): Promise<void> => {
    await apiClient.delete(`/projects/${id}`)
  },

  reorder: async (projectIds: number[]): Promise<void> => {
    await apiClient.patch('/projects/reorder', { projectIds })
  },

  moveToFolder: async (id: number, folderId: number | null): Promise<void> => {
    await apiClient.patch(`/projects/${id}/folder`, { folderId })
  },
}
