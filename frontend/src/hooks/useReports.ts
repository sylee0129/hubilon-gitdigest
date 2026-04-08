import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { reportApi, type ReportQueryParams } from '../services/reportApi'
import type { UpdateSummaryRequest } from '../types/report'

export const REPORT_QUERY_KEY = (params: ReportQueryParams) =>
  ['reports', params] as const

export function useReports(params: ReportQueryParams) {
  const today = new Date().toISOString().split('T')[0]
  const isCurrentPeriod = Boolean(params.endDate && params.endDate >= today)

  return useQuery({
    queryKey: REPORT_QUERY_KEY(params),
    queryFn: () => reportApi.getReports(params),
    enabled: Boolean(params.startDate && params.endDate),
    staleTime: isCurrentPeriod ? 0 : undefined,
  })
}

export function useUpdateSummary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateSummaryRequest }) =>
      reportApi.updateSummary(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['reports'] })
    },
  })
}

export function useGenerateAiSummary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => reportApi.generateAiSummary(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['reports'] })
    },
  })
}

export function useExportExcel() {
  return useMutation({
    mutationFn: (params: ReportQueryParams) => reportApi.exportExcel(params),
    onSuccess: (blob, params) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `주간보고_${params.startDate}_${params.endDate}.xlsx`
      a.click()
      URL.revokeObjectURL(url)
    },
  })
}
