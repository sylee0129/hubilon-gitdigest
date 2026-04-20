import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { folderApi, type FolderCreatePayload, type FolderUpdatePayload, type FolderOrderItem } from '../services/folderApi'
import { useAuthStore } from '../stores/useAuthStore'
import type { Folder } from '../types/folder'

export function useFolders(status?: Folder['status']) {
  const teamId = useAuthStore((s) => s.user?.teamId)
  return useQuery({
    queryKey: ['folders', status, teamId],
    queryFn: () => folderApi.getAll(status),
    enabled: teamId != null,
  })
}

export function useCreateFolder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: FolderCreatePayload) => folderApi.create(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useUpdateFolder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: FolderUpdatePayload }) => folderApi.update(id, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useDeleteFolder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, force }: { id: number; force?: boolean }) => folderApi.delete(id, force),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}

export function useReorderFolders() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (orders: FolderOrderItem[]) => folderApi.reorder(orders),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['folders'] }),
  })
}
