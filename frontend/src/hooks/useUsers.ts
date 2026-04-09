import { useQuery } from '@tanstack/react-query'
import { userApi } from '../services/userApi'

export function useUsers(q?: string) {
  return useQuery({
    queryKey: ['users', q],
    queryFn: () => userApi.search(q),
    enabled: q !== undefined,
    staleTime: 30_000,
  })
}
