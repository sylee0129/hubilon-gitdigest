import { useMutation, useQueryClient } from '@tanstack/react-query'
import { workProjectApi, type WorkProjectCreatePayload, type WorkProjectUpdatePayload, type WorkProjectOrderItem } from '../services/workProjectApi'

export function useCreateWorkProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (p: WorkProjectCreatePayload) => workProjectApi.create(p),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useUpdateWorkProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: WorkProjectUpdatePayload }) => workProjectApi.update(id, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useDeleteWorkProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => workProjectApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useReorderWorkProjects() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ folderId, orders }: { folderId: number; orders: WorkProjectOrderItem[] }) => workProjectApi.reorder(folderId, orders),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}
