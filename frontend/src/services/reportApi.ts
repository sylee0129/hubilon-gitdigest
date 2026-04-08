import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { Report, UpdateSummaryRequest } from '../types/report'

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

  exportExcel: async (params: ReportQueryParams): Promise<Blob> => {
    const { projectId, projectIds, ...rest } = params
    const normalizedParams = {
      ...rest,
      projectIds: projectIds ?? (projectId != null ? [projectId] : undefined),
    }
    const res = await apiClient.get('/reports/export', {
      params: normalizedParams,
      responseType: 'blob',
      paramsSerializer: (p: Record<string, unknown>) => {
        const sp = new URLSearchParams()
        Object.entries(p).forEach(([k, v]) => {
          if (Array.isArray(v)) v.forEach((id) => sp.append(k, String(id)))
          else if (v !== undefined) sp.append(k, String(v))
        })
        return sp.toString()
      },
    })
    return res.data as Blob
  },
}
