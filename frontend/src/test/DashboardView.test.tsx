import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import DashboardView from '../components/dashboard/DashboardView'
import * as useDashboardModule from '../hooks/useDashboard'

vi.mock('../hooks/useDashboard')

function wrapper(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

const mockSummary = {
  totalFolderCount: 10,
  inProgressFolderCount: 4,
  todayCommitCount: 7,
  weeklyCommitCount: 35,
  recentActiveFolders: [
    {
      folderId: 1,
      folderName: '개발팀 폴더',
      commitCount: 5,
      lastCommittedAt: '2026-04-09T12:00:00',
    },
    {
      folderId: 2,
      folderName: '디자인팀 폴더',
      commitCount: 2,
      lastCommittedAt: '2026-04-09T09:30:00',
    },
  ],
}

describe('DashboardView', () => {
  const onFolderSelect = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('로딩 중에는 스켈레톤을 렌더링한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    const { container } = render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(container.querySelectorAll('[class*="statCardSkeleton"]').length).toBeGreaterThan(0)
  })

  it('에러 발생 시 에러 메시지와 새로고침 버튼을 렌더링한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('서버 오류'),
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(screen.getByText('서버 오류')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '새로고침' })).toBeInTheDocument()
  })

  it('데이터 로드 성공 시 통계 카드를 렌더링한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: mockSummary,
      isLoading: false,
      isError: false,
      error: null,
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('총 사업')).toBeInTheDocument()
    expect(screen.getByText('진행중')).toBeInTheDocument()
    expect(screen.getByText('금일 커밋')).toBeInTheDocument()
  })

  it('최근 활성 폴더 목록을 렌더링한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: mockSummary,
      isLoading: false,
      isError: false,
      error: null,
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(screen.getByText('개발팀 폴더')).toBeInTheDocument()
    expect(screen.getByText('디자인팀 폴더')).toBeInTheDocument()
    expect(screen.getByText('커밋 5건')).toBeInTheDocument()
    expect(screen.getByText('커밋 2건')).toBeInTheDocument()
  })

  it('폴더 클릭 시 onFolderSelect가 folderId와 함께 호출된다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: mockSummary,
      isLoading: false,
      isError: false,
      error: null,
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    fireEvent.click(screen.getByText('개발팀 폴더'))
    expect(onFolderSelect).toHaveBeenCalledWith(1)
  })

  it('활성 폴더가 없을 때 빈 상태 메시지를 표시한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: { ...mockSummary, recentActiveFolders: [] },
      isLoading: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(screen.getByText('최근 24시간 내 커밋 활동이 없습니다.')).toBeInTheDocument()
  })

  it('헤더 타이틀을 렌더링한다', () => {
    vi.mocked(useDashboardModule.useDashboardSummary).mockReturnValue({
      data: mockSummary,
      isLoading: false,
      isError: false,
      error: null,
    } as ReturnType<typeof useDashboardModule.useDashboardSummary>)

    render(wrapper(<DashboardView onFolderSelect={onFolderSelect} />))
    expect(screen.getByText('Hubilon GitDigest')).toBeInTheDocument()
    expect(screen.getByText('전체 사업 현황 대시보드')).toBeInTheDocument()
  })
})
