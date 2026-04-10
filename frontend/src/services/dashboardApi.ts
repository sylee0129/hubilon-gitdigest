import axiosInstance from './axios'

export interface RecentActiveFolderItem {
  folderId: number
  folderName: string
  commitCount: number
  lastCommittedAt: string // ISO 8601
}

export interface DashboardSummaryResponse {
  totalFolderCount: number
  inProgressFolderCount: number
  todayCommitCount: number
  weeklyCommitCount: number
  recentActiveFolders: RecentActiveFolderItem[]
}

interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export const dashboardApi = {
  getSummary: () =>
    axiosInstance.get<ApiResponse<DashboardSummaryResponse>>('/dashboard/summary').then(r => r.data.data),
}
