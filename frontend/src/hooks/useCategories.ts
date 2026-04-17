import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi } from '../services/categoryApi'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => categoryApi.getAll(),
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => categoryApi.create(name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

export function useUpdateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => categoryApi.update(id, name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}
