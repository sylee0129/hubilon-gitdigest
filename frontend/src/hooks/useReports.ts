import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { reportApi, type ReportQueryParams } from '../services/reportApi'
import type { UpdateSummaryRequest } from '../types/report'

export function useFolderSummary(params: { folderId: number | null; startDate: string; endDate: string }) {
  return useQuery({
    queryKey: ['folder-summary', params.folderId, params.startDate, params.endDate],
    queryFn: () => reportApi.getFolderSummary({
      folderId: params.folderId!,
      startDate: params.startDate,
      endDate: params.endDate,
    }),
    enabled: params.folderId != null && Boolean(params.startDate && params.endDate),
  })
}

export function useGenerateFolderAiSummary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { folderId: number; startDate: string; endDate: string }) =>
      reportApi.generateFolderAiSummary(payload),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ['folder-summary', variables.folderId],
      })
    },
  })
}

export function useUpdateFolderSummary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: { summary: string } }) =>
      reportApi.updateFolderSummary(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['folder-summary'] })
    },
  })
}

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
