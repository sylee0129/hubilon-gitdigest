import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectApi } from '../services/projectApi'
import type { CreateProjectRequest } from '../types/report'

export const PROJECT_QUERY_KEY = ['projects'] as const

export function useProjects() {
  return useQuery({
    queryKey: PROJECT_QUERY_KEY,
    queryFn: projectApi.getAll,
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateProjectRequest) => projectApi.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: PROJECT_QUERY_KEY })
    },
  })
}

export function useDeleteProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => projectApi.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: PROJECT_QUERY_KEY })
    },
  })
}
