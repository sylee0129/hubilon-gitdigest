import { useState, useEffect } from 'react'
import type { Report } from '../../types/report'
import { useFolderSummary, useGenerateFolderAiSummary, useUpdateFolderSummary } from '../../hooks/useReports'
import { useFolders } from '../../hooks/useFolders'
import { useReportStore } from '../../stores/useReportStore'
import Toast from '../common/Toast'
import styles from './ReportPanel.module.css'

interface FolderReportPanelProps {
  folderId: number
  reports: Report[]
}

export default function FolderReportPanel({ folderId, reports }: FolderReportPanelProps) {
  const { startDate, endDate } = useReportStore()
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [toastVisible, setToastVisible] = useState(false)

  const { data: folders } = useFolders()
  const folderName = folders?.find(f => f.id === folderId)?.name ?? ''

  const folderSummaryQuery = useFolderSummary({ folderId, startDate, endDate })
  const generateFolderAiSummary = useGenerateFolderAiSummary()
  const updateFolderSummary = useUpdateFolderSummary()

  const folderSummary = folderSummaryQuery.data ?? null

  useEffect(() => {
    setDraft(folderSummary?.summary ?? '')
    setIsEditing(false)
  }, [folderSummary?.id, folderId])

  const totalCommitCount = reports.reduce((sum, r) => sum + r.commitCount, 0)
  const uniqueContributorCount = new Set(
    reports.flatMap(r => r.commits.map(c => c.authorEmail))
  ).size

  const handleSave = () => {
    if (!folderSummary) return
    updateFolderSummary.mutate(
      { id: folderSummary.id, payload: { summary: draft } },
      { onSuccess: () => setIsEditing(false) },
    )
  }

  const handleCancel = () => {
    setDraft(folderSummary?.summary ?? '')
    setIsEditing(false)
  }

  const handleGenerateAi = () => {
    generateFolderAiSummary.mutate(
      { folderId, startDate, endDate },
      {
        onSuccess: (updated) => {
          setDraft(updated.summary ?? '')
          setIsEditing(true)
          if (updated.aiSummaryFailed) {
            setToastVisible(true)
          }
        },
      },
    )
  }

  return (
    <>
      <Toast
        message="AI 요약 생성에 실패하여 기본 요약으로 대체되었습니다."
        visible={toastVisible}
        onClose={() => setToastVisible(false)}
      />
      <aside className={styles.panel}>
        <div className={styles.panelHeader}>
          <div className={styles.panelTitle}>
            <span className={styles.panelIcon}>📁</span>
            <span className={styles.panelTitleText}>폴더 보고서</span>
          </div>
          {folderName && (
            <span className={styles.panelProject}>{folderName}</span>
          )}
        </div>

        <div className={styles.panelStats}>
          <span className={styles.statBadge}>커밋 {totalCommitCount}개</span>
          <span className={styles.statBadge}>기여자 {uniqueContributorCount}명</span>
          <span className={styles.statBadge}>{startDate} ~ {endDate}</span>
        </div>

        <div className={styles.panelActions}>
          {isEditing ? (
            <>
              <span className={styles.charCount}>{draft.length}자</span>
              <button
                className={styles.cancelBtn}
                onClick={handleCancel}
                disabled={updateFolderSummary.isPending}
              >
                취소
              </button>
              <button
                className={styles.saveBtn}
                onClick={handleSave}
                disabled={updateFolderSummary.isPending || !folderSummary}
              >
                {updateFolderSummary.isPending ? '저장 중...' : '저장'}
              </button>
            </>
          ) : (
            <>
              <button
                className={styles.aiBtn}
                onClick={handleGenerateAi}
                disabled={generateFolderAiSummary.isPending}
              >
                {generateFolderAiSummary.isPending ? '생성 중...' : '✨ AI 요약'}
              </button>
              <button
                className={styles.editBtn}
                onClick={() => setIsEditing(true)}
                disabled={!folderSummary}
              >
                편집
              </button>
            </>
          )}
        </div>

        <div className={styles.panelContent}>
          {folderSummaryQuery.isLoading ? (
            <span className={styles.summaryEmpty}>불러오는 중...</span>
          ) : isEditing ? (
            <textarea
              className={styles.textarea}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="폴더 요약 내용을 입력하세요..."
              autoFocus
            />
          ) : (
            <div className={styles.summaryContent}>
              {draft
                ? draft.split('\n').map((line, i) => (
                    <p key={i} className={styles.summaryLine}>{line}</p>
                  ))
                : (
                  <span className={styles.summaryEmpty}>
                    요약 내용이 없습니다.<br />
                    AI 요약 또는 직접 편집해 주세요.
                  </span>
                )}
            </div>
          )}
        </div>
      </aside>
    </>
  )
}
