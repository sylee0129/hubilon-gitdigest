import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { confluenceAdminApi } from '../../../../services/confluenceAdminApi'
import type {
  SpaceConfigUpsertRequest,
  TeamConfigUpsertRequest,
} from '../../../../services/confluenceAdminApi'

type SpaceConfigTestPayload = Omit<SpaceConfigUpsertRequest, 'deptId'>

const SPACE_KEY = ['confluence', 'spaces'] as const
const TEAM_KEY = ['confluence', 'teams'] as const

// ─── Space ────────────────────────────────────────────────────────────────────

export function useSpaceConfigs() {
  return useQuery({
    queryKey: SPACE_KEY,
    queryFn: confluenceAdminApi.getSpaceConfigs,
  })
}

export function useUpsertSpaceConfig() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: SpaceConfigUpsertRequest) =>
      confluenceAdminApi.upsertSpaceConfig(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SPACE_KEY })
    },
  })
}

export function useDeleteSpaceConfig() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (deptId: number) => confluenceAdminApi.deleteSpaceConfig(deptId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SPACE_KEY })
    },
  })
}

export function useTestSpaceConnection() {
  return useMutation({
    mutationFn: (deptId: number) => confluenceAdminApi.testSpaceConnection(deptId),
  })
}

export function useTestSpaceConnectionDirect() {
  return useMutation({
    mutationFn: (payload: SpaceConfigTestPayload) =>
      confluenceAdminApi.testSpaceConnectionDirect(payload),
  })
}

// ─── Team ─────────────────────────────────────────────────────────────────────

export function useTeamConfigs() {
  return useQuery({
    queryKey: TEAM_KEY,
    queryFn: confluenceAdminApi.getTeamConfigs,
  })
}

export function useUpsertTeamConfig() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: TeamConfigUpsertRequest) =>
      confluenceAdminApi.upsertTeamConfig(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TEAM_KEY })
    },
  })
}

export function useDeleteTeamConfig() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (teamId: number) => confluenceAdminApi.deleteTeamConfig(teamId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TEAM_KEY })
    },
  })
}
