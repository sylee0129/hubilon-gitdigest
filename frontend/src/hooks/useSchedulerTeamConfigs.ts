import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { schedulerApi, type SchedulerTeamConfig } from '../services/schedulerApi'

const TEAM_CONFIGS_KEY = ['scheduler', 'team-configs'] as const

export function useSchedulerTeamConfigs() {
  return useQuery({
    queryKey: TEAM_CONFIGS_KEY,
    queryFn: schedulerApi.getTeamConfigs,
  })
}

export function useUpdateSchedulerTeamConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ teamId, enabled }: { teamId: number; enabled: boolean }) =>
      schedulerApi.updateTeamConfig(teamId, enabled),
    onMutate: async ({ teamId, enabled }) => {
      await queryClient.cancelQueries({ queryKey: TEAM_CONFIGS_KEY })
      const previous = queryClient.getQueryData<SchedulerTeamConfig[]>(TEAM_CONFIGS_KEY)
      queryClient.setQueryData<SchedulerTeamConfig[]>(TEAM_CONFIGS_KEY, (old) =>
        old?.map((cfg) => (cfg.teamId === teamId ? { ...cfg, enabled } : cfg)) ?? [],
      )
      return { previous }
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(TEAM_CONFIGS_KEY, context.previous)
      }
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: TEAM_CONFIGS_KEY })
    },
  })
}
