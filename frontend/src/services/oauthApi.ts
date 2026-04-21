import apiClient from './axios'
import type { ApiResponse } from '../types/api'

interface GitLabAuthUrlResponse {
  authUrl: string
  state: string
}

interface GitHubAuthUrlResponse {
  authUrl: string
  state: string
}

export const oauthApi = {
  getGitLabAuthUrl: async (gitlabUrl: string): Promise<GitLabAuthUrlResponse> => {
    const res = await apiClient.get<ApiResponse<GitLabAuthUrlResponse>>(
      '/oauth/gitlab/authorize',
      { params: { gitlabUrl } }
    )
    return res.data.data
  },

  getGitHubAuthUrl: async (): Promise<GitHubAuthUrlResponse> => {
    const res = await apiClient.get<ApiResponse<GitHubAuthUrlResponse>>(
      '/oauth/github/authorize'
    )
    return res.data.data
  },
}
