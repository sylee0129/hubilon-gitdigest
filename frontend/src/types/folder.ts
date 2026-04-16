export interface FolderMember {
  id: number
  name: string
  teamName?: string
}

export interface WorkProjectItem {
  id: number
  name: string
  sortOrder: number
}

export interface Folder {
  id: number
  name: string
  category: 'DEVELOPMENT' | 'NEW_BUSINESS' | 'OTHER'
  status: 'IN_PROGRESS' | 'COMPLETED'
  sortOrder: number
  members: FolderMember[]
  workProjects: WorkProjectItem[]
}

export interface User {
  id: number
  name: string
  email: string
  teamId?: number
  teamName?: string
  role?: string
}

export const CATEGORY_LABELS: Record<Folder['category'], string> = {
  DEVELOPMENT: '개발사업',
  NEW_BUSINESS: '신규추진사업',
  OTHER: '기타',
}

export const STATUS_LABELS: Record<Folder['status'], string> = {
  IN_PROGRESS: '진행중',
  COMPLETED: '완료',
}
