export interface Project {
  id: number
  name: string
  gitlabUrl: string
  authType: 'PAT' | 'OAUTH'
  sortOrder: number
  createdAt: string
  folderId?: number | null
}

export interface FileChange {
  oldPath: string
  newPath: string
  newFile: boolean
  renamedFile: boolean
  deletedFile: boolean
  addedLines: number
  removedLines: number
}

export interface CommitInfo {
  id: number
  sha: string
  authorName: string
  authorEmail: string
  committedAt: string
  message: string
  fileChanges: FileChange[]
}

export interface Report {
  id: number
  projectId: number
  projectName: string
  startDate: string
  endDate: string
  commitCount: number
  contributorCount: number
  summary: string
  manuallyEdited: boolean
  commits: CommitInfo[]
  createdAt: string
  aiSummaryFailed: boolean
}

export interface CreateProjectRequest {
  gitlabUrl: string
  gitlabProjectId?: number
  accessToken?: string
  authType: 'PAT' | 'OAUTH'
}

export interface UpdateSummaryRequest {
  summary: string
}

export interface FolderSummary {
  id: number
  folderId: number
  folderName: string
  startDate: string
  endDate: string
  totalCommitCount: number
  uniqueContributorCount: number
  summary: string
  manuallyEdited: boolean
  aiSummaryFailed: boolean
  progressSummary: string | null
  planSummary: string | null
}
