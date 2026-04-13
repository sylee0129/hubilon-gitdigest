import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import React from 'react'

// --- mocks ---
vi.mock('../hooks/useFolders', () => ({
  useFolders: vi.fn(),
}))

vi.mock('../stores/useReportStore', () => ({
  useReportStore: vi.fn(() => ({
    startDate: '2026-04-13',
    endDate: '2026-04-17',
  })),
}))

vi.mock('../services/reportApi', () => ({
  reportApi: {
    getFolderSummary: vi.fn(),
  },
}))

vi.mock('../utils/weeklyExcelExport', () => ({
  exportWeeklyExcel: vi.fn().mockResolvedValue(undefined),
}))

import { useFolders } from '../hooks/useFolders'
import { reportApi } from '../services/reportApi'
import { exportWeeklyExcel } from '../utils/weeklyExcelExport'
import { useWeeklyExcelDownload } from '../hooks/useWeeklyExcelDownload'

const mockFolders = [
  {
    id: 1,
    name: '개발프로젝트',
    category: 'DEVELOPMENT' as const,
    members: [{ name: '홍길동' }],
  },
  {
    id: 2,
    name: '신사업프로젝트',
    category: 'NEW_BUSINESS' as const,
    members: [{ name: '김철수' }],
  },
]

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

describe('useWeeklyExcelDownload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useFolders).mockReturnValue({
      data: mockFolders,
    } as ReturnType<typeof useFolders>)
  })

  it('초기 상태: loading = false', () => {
    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    expect(result.current.loading).toBe(false)
  })

  it('folders 없으면 download() 호출 시 exportWeeklyExcel 미호출', async () => {
    vi.mocked(useFolders).mockReturnValue({
      data: [],
    } as unknown as ReturnType<typeof useFolders>)

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    expect(exportWeeklyExcel).not.toHaveBeenCalled()
  })

  it('FolderSummary null 시 fallback 문자열 적용', async () => {
    vi.mocked(reportApi.getFolderSummary)
      .mockResolvedValueOnce(null as any)   // folder 1 → null
      .mockResolvedValueOnce({ progressSummary: '완료', planSummary: '없음' } as any) // folder 2

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    const callArgs = vi.mocked(exportWeeklyExcel).mock.calls[0][0]
    const row1 = callArgs.rows.find((r) => r.folderName === '개발프로젝트')
    expect(row1?.progressSummary).toBe('진행사항 없음')
    expect(row1?.planSummary).toBe('진행사항 확인')
  })

  it('FolderSummary 정상 시 값 그대로 사용', async () => {
    vi.mocked(reportApi.getFolderSummary)
      .mockResolvedValueOnce({ progressSummary: 'API 구현', planSummary: '테스트 예정' } as any)
      .mockResolvedValueOnce({ progressSummary: '기획', planSummary: '디자인' } as any)

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    const callArgs = vi.mocked(exportWeeklyExcel).mock.calls[0][0]
    const row1 = callArgs.rows.find((r) => r.folderName === '개발프로젝트')
    expect(row1?.progressSummary).toBe('API 구현')
    expect(row1?.planSummary).toBe('테스트 예정')
  })

  it('getFolderSummary rejected 시 해당 폴더 rows에서 제외', async () => {
    vi.mocked(reportApi.getFolderSummary)
      .mockRejectedValueOnce(new Error('네트워크 오류'))
      .mockResolvedValueOnce({ progressSummary: '기획', planSummary: '디자인' } as any)

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    const callArgs = vi.mocked(exportWeeklyExcel).mock.calls[0][0]
    expect(callArgs.rows).toHaveLength(1)
    expect(callArgs.rows[0].folderName).toBe('신사업프로젝트')
  })

  it('download 완료 후 loading이 false로 복귀', async () => {
    vi.mocked(reportApi.getFolderSummary).mockResolvedValue({ progressSummary: '', planSummary: '' } as any)

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    expect(result.current.loading).toBe(false)
  })

  it('exportWeeklyExcel에 startDate, endDate가 정확히 전달된다', async () => {
    vi.mocked(reportApi.getFolderSummary).mockResolvedValue({ progressSummary: '', planSummary: '' } as any)

    const { result } = renderHook(() => useWeeklyExcelDownload(), { wrapper })
    await act(async () => { await result.current.download() })

    const callArgs = vi.mocked(exportWeeklyExcel).mock.calls[0][0]
    expect(callArgs.startDate).toBe('2026-04-13')
    expect(callArgs.endDate).toBe('2026-04-17')
  })
})
