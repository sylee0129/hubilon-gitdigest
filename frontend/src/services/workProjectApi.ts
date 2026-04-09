import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { WorkProjectItem } from '../types/folder'

export interface WorkProjectCreatePayload { folderId: number; name: string }
export interface WorkProjectUpdatePayload { folderId: number; name: string }
export interface WorkProjectOrderItem { id: number; sortOrder: number }

export const workProjectApi = {
  create: async (payload: WorkProjectCreatePayload): Promise<WorkProjectItem> => {
    const res = await apiClient.post<ApiResponse<WorkProjectItem>>('/work-projects', payload)
    return res.data.data
  },
  update: async (id: number, payload: WorkProjectUpdatePayload): Promise<WorkProjectItem> => {
    const res = await apiClient.put<ApiResponse<WorkProjectItem>>(`/work-projects/${id}`, payload)
    return res.data.data
  },
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/work-projects/${id}`)
  },
  reorder: async (folderId: number, orders: WorkProjectOrderItem[]): Promise<void> => {
    await apiClient.patch('/work-projects/reorder', { folderId, orders })
  },
}
