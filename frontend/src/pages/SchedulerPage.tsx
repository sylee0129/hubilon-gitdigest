import { useState, useEffect, useCallback } from 'react'
import Header from '../components/layout/Header'
import SidebarLayout from '../components/layout/SidebarLayout'
import Toast from '../components/common/Toast'
import SchedulerTeamSettingsTab from '../components/scheduler/SchedulerTeamSettingsTab'
import { schedulerApi, type SchedulerLog, type SchedulerLogDetail, type SchedulerStatus } from '../services/schedulerApi'
import { useSchedulerTeamConfigs } from '../hooks/useSchedulerTeamConfigs'
import { useAuthStore } from '../stores/useAuthStore'
import styles from './SchedulerPage.module.css'

// ─── 상태 뱃지 ────────────────────────────────────────────────────────────────

const STATUS_LABEL: Record<SchedulerStatus, string> = {
  RUNNING: '실행 중',
  SUCCESS: '성공',
  PARTIAL_FAIL: '일부 실패',
  FAIL: '실패',
}

const STATUS_CLASS: Record<SchedulerStatus, string> = {
  RUNNING: styles.statusRunning,
  SUCCESS: styles.statusSuccess,
  PARTIAL_FAIL: styles.statusPartialFail,
  FAIL: styles.statusFail,
}

interface StatusBadgeProps {
  status: SchedulerStatus
}

function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`${styles.statusBadge} ${STATUS_CLASS[status]}`}>
      {status === 'RUNNING' && <span className={styles.runningDot} />}
      {STATUS_LABEL[status]}
    </span>
  )
}

// ─── 날짜 포맷 ────────────────────────────────────────────────────────────────

function formatDateTime(iso: string): string {
  const d = new Date(iso)
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${mo}-${day} ${h}:${mi}`
}

// ─── 상세 모달 ────────────────────────────────────────────────────────────────

interface DetailModalProps {
  detail: SchedulerLogDetail
  onClose: () => void
}

function DetailModal({ detail, onClose }: DetailModalProps) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose])

  const confluenceUrl = detail.folderResults.find((fr) => fr.success && fr.confluencePageUrl)?.confluencePageUrl ?? null

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.modalHeader}>
          <div className={styles.modalTitle}>실행 상세</div>
          <button className={styles.modalClose} onClick={onClose}>✕</button>
        </div>

        <div className={styles.modalMeta}>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>실행 시각</span>
            <span className={styles.metaValue}>{formatDateTime(detail.executedAt)}</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>상태</span>
            <StatusBadge status={detail.status} />
          </div>
          {detail.teamName && (
            <div className={styles.metaItem}>
              <span className={styles.metaLabel}>팀</span>
              <span className={styles.metaValue}>{detail.teamName}</span>
            </div>
          )}
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>대상 폴더</span>
            <span className={styles.metaValue}>{detail.totalFolderCount}개</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>성공</span>
            <span className={`${styles.metaValue} ${styles.successText}`}>{detail.successCount}개</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>실패</span>
            <span className={`${styles.metaValue} ${detail.failCount > 0 ? styles.failText : ''}`}>
              {detail.failCount}개
            </span>
          </div>
        </div>

        {confluenceUrl && (
          <a
            href={confluenceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.confluenceBanner}
            onClick={(e) => e.stopPropagation()}
          >
            <span className={styles.confluenceBannerIcon}>📄</span>
            <span>Confluence 주간보고 페이지 열기</span>
            <span className={styles.confluenceBannerArrow}>→</span>
          </a>
        )}

        <div className={styles.divider} />

        <div className={styles.folderResultList}>
          {detail.folderResults.map((fr) => (
            <div
              key={fr.folderId}
              className={`${styles.folderResultItem} ${fr.success ? styles.folderResultSuccess : styles.folderResultFail}`}
            >
              <div className={styles.folderResultLeft}>
                <span className={fr.success ? styles.resultIconSuccess : styles.resultIconFail}>
                  {fr.success ? '✓' : '✗'}
                </span>
                <span className={styles.folderResultName}>{fr.folderName}</span>
              </div>
              {!fr.success && fr.errorMessage && (
                <div className={styles.folderResultRight}>
                  <span className={styles.errorMessage}>{fr.errorMessage}</span>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ─── 수동 실행 영역 ───────────────────────────────────────────────────────────

type ActiveTab = 'logs' | 'team-settings'

interface TriggerSectionProps {
  isAdmin: boolean
  isTriggerLoading: boolean
  onTrigger: (teamId?: number) => void
}

function TriggerSection({ isAdmin, isTriggerLoading, onTrigger }: TriggerSectionProps) {
  const { data: teamConfigs } = useSchedulerTeamConfigs()
  const [selectedTeamId, setSelectedTeamId] = useState<number | ''>('')

  useEffect(() => {
    if (isAdmin && teamConfigs && teamConfigs.length > 0 && selectedTeamId === '') {
      setSelectedTeamId(teamConfigs[0].teamId)
    }
  }, [isAdmin, teamConfigs, selectedTeamId])

  const handleClick = () => {
    if (isAdmin && selectedTeamId !== '') {
      onTrigger(selectedTeamId as number)
    } else {
      onTrigger()
    }
  }

  return (
    <div className={styles.triggerSection}>
      {isAdmin && (
        <select
          className={styles.teamSelect}
          value={selectedTeamId}
          onChange={(e) => setSelectedTeamId(e.target.value === '' ? '' : Number(e.target.value))}
          disabled={isTriggerLoading}
        >
          <option value="">팀 선택</option>
          {teamConfigs?.map((cfg) => (
            <option key={cfg.teamId} value={cfg.teamId}>
              {cfg.teamName}
            </option>
          ))}
        </select>
      )}
      <button
        className={styles.triggerBtn}
        onClick={handleClick}
        disabled={isTriggerLoading}
      >
        {isTriggerLoading ? '실행 중...' : '수동 실행'}
      </button>
    </div>
  )
}

// ─── SchedulerPage ────────────────────────────────────────────────────────────

export default function SchedulerPage() {
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === 'ADMIN'

  const [activeTab, setActiveTab] = useState<ActiveTab>('logs')

  const [logs, setLogs] = useState<SchedulerLog[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [isError, setIsError] = useState(false)

  const [isTriggerLoading, setIsTriggerLoading] = useState(false)

  const [selectedDetail, setSelectedDetail] = useState<SchedulerLogDetail | null>(null)
  const [isDetailLoading, setIsDetailLoading] = useState(false)

  const [toast, setToast] = useState({ visible: false, message: '' })

  const showToast = (message: string) => {
    setToast({ visible: true, message })
  }

  const fetchLogs = useCallback(async (p: number) => {
    setIsLoading(true)
    setIsError(false)
    try {
      const data = await schedulerApi.getLogs(p)
      setLogs(data.content)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
    } catch {
      setIsError(true)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void fetchLogs(page)
  }, [page, fetchLogs])

  const handleTrigger = async (teamId?: number) => {
    setIsTriggerLoading(true)
    try {
      await schedulerApi.trigger(teamId)
      showToast('스케줄러 실행이 완료되었습니다.')
      setPage(0)
      await fetchLogs(0)
    } catch (err) {
      const message = err instanceof Error ? err.message : '오류가 발생했습니다.'
      if (message.includes('이미 실행 중')) {
        showToast('이미 실행 중입니다.')
      } else {
        showToast(message)
      }
    } finally {
      setIsTriggerLoading(false)
    }
  }

  const handleOpenDetail = async (id: number) => {
    setIsDetailLoading(true)
    try {
      const detail = await schedulerApi.getLogDetail(id)
      setSelectedDetail(detail)
    } catch (err) {
      const message = err instanceof Error ? err.message : '상세 정보를 불러오지 못했습니다.'
      showToast(message)
    } finally {
      setIsDetailLoading(false)
    }
  }

  return (
    <div className={styles.layout}>
      <Header />

      <SidebarLayout>
        <main className={styles.main}>
          <div className={styles.pageHeader}>
            <h1 className={styles.pageTitle}>주간보고 스케줄러</h1>
            <TriggerSection
              isAdmin={isAdmin}
              isTriggerLoading={isTriggerLoading}
              onTrigger={(teamId) => void handleTrigger(teamId)}
            />
          </div>

          {isAdmin && (
            <div className={styles.tabList}>
              <button
                className={`${styles.tabBtn} ${activeTab === 'logs' ? styles.tabActive : ''}`}
                onClick={() => setActiveTab('logs')}
              >
                실행 이력
              </button>
              <button
                className={`${styles.tabBtn} ${activeTab === 'team-settings' ? styles.tabActive : ''}`}
                onClick={() => setActiveTab('team-settings')}
              >
                팀별 설정
              </button>
            </div>
          )}

          {activeTab === 'team-settings' && isAdmin ? (
            <div className={styles.tableWrapper}>
              <SchedulerTeamSettingsTab />
            </div>
          ) : (
            <>
              {isLoading && (
                <div className={styles.stateContainer}>
                  <div className={styles.spinner} />
                  <span>불러오는 중...</span>
                </div>
              )}

              {isError && (
                <div className={styles.stateContainer}>
                  <span className={styles.errorText}>실행 이력을 불러오지 못했습니다.</span>
                </div>
              )}

              {!isLoading && !isError && (
                <>
                  <div className={styles.tableWrapper}>
                    <table className={styles.table}>
                      <thead>
                        <tr>
                          <th>실행 시각</th>
                          <th>팀</th>
                          <th>상태</th>
                          <th>대상 폴더</th>
                          <th>성공</th>
                          <th>실패</th>
                          <th>상세</th>
                        </tr>
                      </thead>
                      <tbody>
                        {logs.length === 0 ? (
                          <tr>
                            <td colSpan={7} className={styles.emptyCell}>
                              실행 이력이 없습니다.
                            </td>
                          </tr>
                        ) : (
                          logs.map((log) => (
                            <tr key={log.id}>
                              <td>{formatDateTime(log.executedAt)}</td>
                              <td className={styles.teamCell}>{log.teamName ?? '-'}</td>
                              <td>
                                <StatusBadge status={log.status} />
                              </td>
                              <td>{log.totalFolderCount}</td>
                              <td className={styles.successText}>{log.successCount}</td>
                              <td className={log.failCount > 0 ? styles.failText : ''}>{log.failCount}</td>
                              <td>
                                <button
                                  className={styles.detailBtn}
                                  onClick={() => void handleOpenDetail(log.id)}
                                  disabled={isDetailLoading}
                                >
                                  상세
                                </button>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>

                  {totalPages > 1 && (
                    <div className={styles.pagination}>
                      <button
                        className={styles.pageBtn}
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                      >
                        이전
                      </button>
                      <span className={styles.pageInfo}>
                        {page + 1} / {totalPages}
                        <span className={styles.totalCount}>({totalElements}건)</span>
                      </span>
                      <button
                        className={styles.pageBtn}
                        disabled={page >= totalPages - 1}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        다음
                      </button>
                    </div>
                  )}
                </>
              )}
            </>
          )}
        </main>
      </SidebarLayout>

      {selectedDetail && (
        <DetailModal
          detail={selectedDetail}
          onClose={() => setSelectedDetail(null)}
        />
      )}

      <Toast
        message={toast.message}
        visible={toast.visible}
        onClose={() => setToast((t) => ({ ...t, visible: false }))}
      />
    </div>
  )
}
