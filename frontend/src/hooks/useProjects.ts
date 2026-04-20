import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectApi } from '../services/projectApi'
import { useAuthStore } from '../stores/useAuthStore'
import type { CreateProjectRequest, Project } from '../types/report'

export function useProjects() {
  const teamId = useAuthStore((s) => s.user?.teamId)
  return useQuery({
    queryKey: ['projects', teamId] as const,
    queryFn: projectApi.getAll,
    enabled: teamId != null,
  })
}

export const PROJECT_QUERY_KEY = ['projects'] as const

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

export function useMoveProjectToFolder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, folderId }: { id: number; folderId: number | null }) =>
      projectApi.moveToFolder(id, folderId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: PROJECT_QUERY_KEY })
    },
  })
}

export function useReorderProjects() {
  const queryClient = useQueryClient()
  const teamId = useAuthStore((s) => s.user?.teamId)
  const queryKey = ['projects', teamId] as const
  return useMutation({
    mutationFn: (projectIds: number[]) => projectApi.reorder(projectIds),
    onMutate: async (projectIds: number[]) => {
      await queryClient.cancelQueries({ queryKey: PROJECT_QUERY_KEY })
      const previous = queryClient.getQueryData<Project[]>(queryKey)
      queryClient.setQueryData<Project[]>(queryKey, (old) => {
        if (!old) return old
        return projectIds
          .map((id) => old.find((p) => p.id === id))
          .filter((p): p is Project => p !== undefined)
      })
      return { previous }
    },
    onError: (_err, _ids, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKey, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: PROJECT_QUERY_KEY })
    },
  })
}
