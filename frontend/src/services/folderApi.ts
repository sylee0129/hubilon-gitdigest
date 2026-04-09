import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Folder } from '../types/folder'

export interface FolderCreatePayload {
  name: string
  category: Folder['category']
  status: Folder['status']
  memberIds: number[]
}

export interface FolderUpdatePayload {
  name: string
  category: Folder['category']
  status: Folder['status']
  memberIds: number[]
}

export interface FolderOrderItem {
  id: number
  sortOrder: number
}

export const folderApi = {
  getAll: async (status?: Folder['status']): Promise<Folder[]> => {
    const res = await apiClient.get<ApiResponse<Folder[]>>('/folders', { params: status ? { status } : undefined })
    return res.data.data
  },
  create: async (payload: FolderCreatePayload): Promise<Folder> => {
    const res = await apiClient.post<ApiResponse<Folder>>('/folders', payload)
    return res.data.data
  },
  update: async (id: number, payload: FolderUpdatePayload): Promise<Folder> => {
    const res = await apiClient.put<ApiResponse<Folder>>(`/folders/${id}`, payload)
    return res.data.data
  },
  delete: async (id: number, force = false): Promise<void> => {
    await apiClient.delete(`/folders/${id}`, { params: { force } })
  },
  reorder: async (orders: FolderOrderItem[]): Promise<void> => {
    await apiClient.patch('/folders/reorder', { orders })
  },
}
