import { useState, useRef, useEffect } from 'react'
import { useReportStore } from '../stores/useReportStore'
import { useReports } from '../hooks/useReports'
import { useProjects } from '../hooks/useProjects'
import Header from '../components/layout/Header'
import Sidebar from '../components/layout/Sidebar'
import ReportCard from '../components/report/ReportCard'
import FolderReportPanel from '../components/report/FolderReportPanel'
import DashboardView from '../components/dashboard/DashboardView'
import styles from './ReportDashboard.module.css'

const SIDEBAR_MIN = 160
const SIDEBAR_MAX = 480

export default function ReportDashboard() {
  const { startDate, endDate, activeTab, selectedProjectId, selectedFolderId, setSelectedFolder } = useReportStore()
  const { data: projects } = useProjects()

  const [sidebarWidth, setSidebarWidth] = useState(240)
  const isResizing = useRef(false)

  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      if (!isResizing.current) return
      const next = Math.min(SIDEBAR_MAX, Math.max(SIDEBAR_MIN, e.clientX))
      setSidebarWidth(next)
    }
    const onMouseUp = () => { isResizing.current = false }
    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    return () => {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
    }
  }, [])

  const targetProjects = selectedFolderId
    ? projects?.filter(p => p.folderId === selectedFolderId) ?? []
    : projects ?? []

  const isFolderEmpty = selectedFolderId != null && targetProjects.length === 0

  const reportsQuery = useReports({
    startDate,
    endDate,
    projectId: activeTab === 'individual' && selectedProjectId != null
      ? selectedProjectId
      : undefined,
    projectIds: activeTab === 'all' && selectedFolderId != null
      ? targetProjects.map(p => p.id)
      : undefined,
  })

  return (
    <div className={styles.layout}>
      <Header />

      <div className={styles.body}>
        <Sidebar width={sidebarWidth} />

        <div
          className={styles.resizeHandle}
          onMouseDown={() => { isResizing.current = true }}
        />

        <main className={styles.main}>
          {selectedFolderId == null && selectedProjectId == null ? (
            <div className={styles.dashboardWrapper}>
              <DashboardView onFolderSelect={(folderId) => setSelectedFolder(folderId)} />
            </div>
          ) : (
            <>
              <div className={styles.contentWrapper}>
                <div className={styles.cardColumn}>
                  {isFolderEmpty ? (
                    <div className={styles.stateContainer}>
                      <span className={styles.emptyText}>이 폴더에 등록된 프로젝트가 없습니다.</span>
                    </div>
                  ) : (
                    <>
                      {reportsQuery.isLoading && (
                        <div className={styles.stateContainer}>
                          <div className={styles.spinner} />
                          <span>보고서를 불러오는 중...</span>
                        </div>
                      )}

                      {reportsQuery.isError && (
                        <div className={styles.stateContainer}>
                          <span className={styles.errorText}>
                            {reportsQuery.error instanceof Error
                              ? reportsQuery.error.message
                              : '보고서를 불러오지 못했습니다.'}
                          </span>
                        </div>
                      )}

                      {reportsQuery.isSuccess && reportsQuery.data.length === 0 && (
                        <div className={styles.stateContainer}>
                          <span className={styles.emptyText}>
                            {activeTab === 'individual' && selectedProjectId == null
                              ? '사이드바에서 프로젝트를 선택해 주세요.'
                              : '해당 기간에 보고서가 없습니다.'}
                          </span>
                        </div>
                      )}

                      {reportsQuery.isSuccess && reportsQuery.data.length > 0 && (
                        <div className={styles.cardList}>
                          {reportsQuery.data.map((report) => (
                            <ReportCard
                              key={report.id}
                              report={report}
                            />
                          ))}
                        </div>
                      )}
                    </>
                  )}
                </div>

                {selectedFolderId != null && activeTab === 'all' ? (
                  <FolderReportPanel
                    folderId={selectedFolderId}
                    reports={reportsQuery.data ?? []}
                  />
                ) : selectedProjectId == null ? (
                  <div className={styles.panelPlaceholder}>
                    <span className={styles.panelPlaceholderText}>
                      사업을 선택하면 AI 요약 보고서가 여기에 생성됩니다.
                    </span>
                  </div>
                ) : null}
              </div>

            </>
          )}
        </main>
      </div>
    </div>
  )
}
