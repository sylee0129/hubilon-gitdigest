import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Report, UpdateSummaryRequest, FolderSummary } from '../types/report'

export interface ReportQueryParams {
  projectId?: number
  projectIds?: number[]
  startDate: string
  endDate: string
}

export const reportApi = {
  getReports: async (params: ReportQueryParams): Promise<Report[]> => {
    const res = await apiClient.get<ApiResponse<Report[]>>('/reports', { params })
    return res.data.data
  },

  generateAiSummary: async (id: number): Promise<Report> => {
    const res = await apiClient.post<ApiResponse<Report>>(
      `/reports/${id}/ai-summary`,
      undefined,
      { timeout: 60_000 },
    )
    return res.data.data
  },

  isAiSummaryFailed: (report: Report): boolean => report.aiSummaryFailed ?? false,

  updateSummary: async (id: number, payload: UpdateSummaryRequest): Promise<Report> => {
    const res = await apiClient.put<ApiResponse<Report>>(`/reports/${id}/summary`, payload)
    return res.data.data
  },

  getFolderSummary: async (params: { folderId: number; startDate: string; endDate: string }): Promise<FolderSummary | null> => {
    const res = await apiClient.get<ApiResponse<FolderSummary | null>>('/reports/folder-summary', { params })
    return res.data.data
  },

  generateFolderAiSummary: async (payload: { folderId: number; startDate: string; endDate: string }): Promise<FolderSummary> => {
    const res = await apiClient.post<ApiResponse<FolderSummary>>(
      '/reports/folder-summary/ai-summary',
      payload,
      { timeout: 60_000 },
    )
    return res.data.data
  },

  previewFolderAiSummary: async (payload: { folderId: number; startDate: string; endDate: string }): Promise<{ progressSummary: string; planSummary: string; aiSummaryFailed: boolean }> => {
    const res = await apiClient.post<ApiResponse<{ progressSummary: string; planSummary: string; aiSummaryFailed: boolean }>>(
      '/reports/folder-summary/ai-preview',
      payload,
      { timeout: 60_000 },
    )
    return res.data.data
  },

  createFolderSummary: async (payload: { folderId: number; startDate: string; endDate: string; progressSummary?: string | null; planSummary?: string | null }): Promise<FolderSummary> => {
    const res = await apiClient.post<ApiResponse<FolderSummary>>('/reports/folder-summary', payload)
    return res.data.data
  },

  updateFolderSummary: async (id: number, payload: {
    summary?: string
    progressSummary?: string | null
    planSummary?: string | null
  }): Promise<FolderSummary> => {
    const res = await apiClient.put<ApiResponse<FolderSummary>>(`/reports/folder-summary/${id}/summary`, payload)
    return res.data.data
  },

}
