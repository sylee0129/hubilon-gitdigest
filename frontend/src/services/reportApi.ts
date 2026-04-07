import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Report, UpdateSummaryRequest } from '../types/report'

export interface ReportQueryParams {
  projectId?: number
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

  updateSummary: async (id: number, payload: UpdateSummaryRequest): Promise<Report> => {
    const res = await apiClient.put<ApiResponse<Report>>(`/reports/${id}/summary`, payload)
    return res.data.data
  },

  exportExcel: async (params: ReportQueryParams): Promise<Blob> => {
    const res = await apiClient.get('/reports/export', {
      params,
      responseType: 'blob',
    })
    return res.data as Blob
  },
}
