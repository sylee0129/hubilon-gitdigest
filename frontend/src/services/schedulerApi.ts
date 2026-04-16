import apiClient from './axios'
import type { ApiResponse } from '../types/api'

export type SchedulerStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAIL' | 'FAIL'

export interface SchedulerLog {
  id: number
  executedAt: string
  status: SchedulerStatus
  totalFolderCount: number
  successCount: number
  failCount: number
}

export interface SchedulerLogPage {
  content: SchedulerLog[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface FolderResult {
  folderId: number
  folderName: string
  success: boolean
  confluencePageUrl: string | null
  errorMessage: string | null
}

export interface SchedulerLogDetail extends SchedulerLog {
  folderResults: FolderResult[]
}

export const schedulerApi = {
  getLogs: async (page = 0, size = 10): Promise<SchedulerLogPage> => {
    const res = await apiClient.get<ApiResponse<SchedulerLogPage>>('/scheduler/logs', { params: { page, size } })
    return res.data.data
  },

  getLogDetail: async (id: number): Promise<SchedulerLogDetail> => {
    const res = await apiClient.get<ApiResponse<SchedulerLogDetail>>(`/scheduler/logs/${id}`)
    return res.data.data
  },

  trigger: async (): Promise<SchedulerLogDetail> => {
    const res = await apiClient.post<ApiResponse<SchedulerLogDetail>>('/scheduler/trigger')
    return res.data.data
  },
}
