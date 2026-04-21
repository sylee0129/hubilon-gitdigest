import apiClient from './axios'
import type { ApiResponse } from '../types/api'

// ─── Types ────────────────────────────────────────────────────────────────────

export interface SpaceConfig {
  id: number
  deptId: number
  deptName: string
  userEmail: string
  apiToken: string
  spaceKey: string
  baseUrl: string
  updatedBy: string
  updatedAt: string
}

export interface TeamConfig {
  id: number
  teamId: number
  teamName: string
  parentPageId: string
  updatedBy: string
  updatedAt: string
}

export interface SpaceConfigUpsertRequest {
  deptId: number
  userEmail: string
  apiToken: string
  spaceKey: string
  baseUrl: string
}

export interface TeamConfigUpsertRequest {
  teamId: number
  parentPageId: string
}

// ─── API ──────────────────────────────────────────────────────────────────────

export const confluenceAdminApi = {
  // Space
  getSpaceConfigs: async (): Promise<SpaceConfig[]> => {
    const res = await apiClient.get<ApiResponse<SpaceConfig[]>>('/admin/confluence/spaces')
    return res.data.data ?? []
  },

  upsertSpaceConfig: async (payload: SpaceConfigUpsertRequest): Promise<SpaceConfig> => {
    const res = await apiClient.post<ApiResponse<SpaceConfig>>('/admin/confluence/spaces', payload)
    return res.data.data
  },

  deleteSpaceConfig: async (deptId: number): Promise<void> => {
    await apiClient.delete(`/admin/confluence/spaces/${deptId}`)
  },

  testSpaceConnection: async (deptId: number): Promise<{ result: string }> => {
    const res = await apiClient.post<ApiResponse<{ result: string }>>(
      `/admin/confluence/spaces/${deptId}/test`
    )
    return res.data.data
  },

  testSpaceConnectionDirect: async (payload: Omit<SpaceConfigUpsertRequest, 'deptId'>): Promise<{ result: string }> => {
    const res = await apiClient.post<ApiResponse<{ result: string }>>(
      '/admin/confluence/spaces/test',
      payload
    )
    return res.data.data
  },

  // Team
  getTeamConfigs: async (): Promise<TeamConfig[]> => {
    const res = await apiClient.get<ApiResponse<TeamConfig[]>>('/admin/confluence/teams')
    return res.data.data ?? []
  },

  upsertTeamConfig: async (payload: TeamConfigUpsertRequest): Promise<TeamConfig> => {
    const res = await apiClient.post<ApiResponse<TeamConfig>>('/admin/confluence/teams', payload)
    return res.data.data
  },

  deleteTeamConfig: async (teamId: number): Promise<void> => {
    await apiClient.delete(`/admin/confluence/teams/${teamId}`)
  },
}
