import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectApi } from '../services/projectApi'
import type { CreateProjectRequest, Project } from '../types/report'

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

export function useReorderProjects() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (projectIds: number[]) => projectApi.reorder(projectIds),
    onMutate: async (projectIds: number[]) => {
      await queryClient.cancelQueries({ queryKey: PROJECT_QUERY_KEY })
      const previous = queryClient.getQueryData<Project[]>(PROJECT_QUERY_KEY)
      queryClient.setQueryData<Project[]>(PROJECT_QUERY_KEY, (old) => {
        if (!old) return old
        return projectIds
          .map((id) => old.find((p) => p.id === id))
          .filter((p): p is Project => p !== undefined)
      })
      return { previous }
    },
    onError: (_err, _ids, context) => {
      if (context?.previous) {
        queryClient.setQueryData(PROJECT_QUERY_KEY, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: PROJECT_QUERY_KEY })
    },
  })
}
