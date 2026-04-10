import { useQueryClient } from '@tanstack/react-query'
import { useDashboardSummary } from '../../hooks/useDashboard'
import styles from './DashboardView.module.css'

interface DashboardViewProps {
  onFolderSelect: (folderId: number) => void
}

export default function DashboardView({ onFolderSelect }: DashboardViewProps) {
  const queryClient = useQueryClient()
  const { data, isLoading, isError, error } = useDashboardSummary()

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ['dashboard', 'summary'] })
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Hubilon GitDigest</h1>
        <p className={styles.subtitle}>전체 사업 현황 대시보드</p>
      </div>

      {/* 통계 카드 */}
      <div className={styles.statsRow}>
        {isLoading ? (
          <>
            <div className={styles.statCardSkeleton} />
            <div className={styles.statCardSkeleton} />
            <div className={styles.statCardSkeleton} />
          </>
        ) : isError ? (
          <div className={styles.errorState}>
            <span className={styles.errorText}>
              {error instanceof Error ? error.message : '데이터를 불러오지 못했습니다.'}
            </span>
            <button className={styles.refreshBtn} onClick={handleRefresh}>
              새로고침
            </button>
          </div>
        ) : (
          <>
            <div className={styles.statCard}>
              <span className={styles.statBadge}>전체</span>
              <span className={styles.statNumber}>{data?.totalFolderCount ?? 0}</span>
              <span className={styles.statLabel}>총 사업</span>
            </div>
            <div className={styles.statCard}>
              <span className={`${styles.statBadge} ${styles.statBadgeActive}`}>진행</span>
              <span className={styles.statNumber}>{data?.inProgressFolderCount ?? 0}</span>
              <span className={styles.statLabel}>진행중</span>
            </div>
            <div className={styles.statCard}>
              <span className={`${styles.statBadge} ${styles.statBadgeToday}`}>오늘</span>
              <span className={styles.statNumber}>{data?.todayCommitCount ?? 0}</span>
              <span className={styles.statLabel}>금일 커밋</span>
            </div>
          </>
        )}
      </div>

      {/* 최근 활성 폴더 */}
      <div className={styles.section}>
        <h2 className={styles.sectionTitle}>최근 업데이트 사업 (24시간)</h2>

        {isLoading ? (
          <div className={styles.listSkeleton}>
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className={styles.listItemSkeleton} />
            ))}
          </div>
        ) : isError ? null : (
          <div className={styles.folderList}>
            {!data?.recentActiveFolders?.length ? (
              <div className={styles.emptyState}>최근 24시간 내 커밋 활동이 없습니다.</div>
            ) : (
              data.recentActiveFolders.map(item => (
                <button
                  key={item.folderId}
                  className={styles.folderItem}
                  onClick={() => onFolderSelect(item.folderId)}
                >
                  <span className={styles.folderName}>{item.folderName}</span>
                  <span className={styles.commitBadge}>커밋 {item.commitCount}건</span>
                  <span className={styles.commitTime}>
                    {new Date(item.lastCommittedAt).toLocaleString('ko-KR')}
                  </span>
                </button>
              ))
            )}
          </div>
        )}
      </div>

      {/* 가이드 섹션 */}
      <div className={styles.guide}>
        <span className={styles.guideIcon}>💡</span>
        <p className={styles.guideText}>
          분석할 사업 폴더나 개별 프로젝트를 왼쪽 사이드바에서 선택하여
          상세 이력과 AI 보고서를 확인하세요.
        </p>
      </div>
    </div>
  )
}
