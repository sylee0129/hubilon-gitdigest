import apiClient from './axios'
import type { ApiResponse } from '../types/api'
import type { WeeklyReportRow } from '../utils/weeklyExcelExport'

export const confluenceApi = {
  uploadWeeklyReport: async (params: {
    rows: WeeklyReportRow[]
    startDate: string
    endDate: string
  }): Promise<{ pageUrl: string }> => {
    const res = await apiClient.post<ApiResponse<{ pageUrl: string }>>(
      '/confluence/weekly-report',
      params
    )
    return res.data.data
  },
}
