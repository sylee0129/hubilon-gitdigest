import { useState } from 'react'
import type { Report } from '../../types/report'
import { useFolderSummary, usePreviewFolderAiSummary, useCreateFolderSummary, useUpdateFolderSummary } from '../../hooks/useReports'
import { useFolderSummaryEditor, stripSectionHeader } from '../../hooks/useFolderSummaryEditor'
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
  const [toast, setToast] = useState<{ visible: boolean; message: string }>({ visible: false, message: '' })
  const showToast = (message: string) => setToast({ visible: true, message })
  const hideToast = () => setToast(prev => ({ ...prev, visible: false }))

  const { data: folders } = useFolders()
  const folderName = folders?.find(f => f.id === folderId)?.name ?? ''

  const folderSummaryQuery = useFolderSummary({ folderId, startDate, endDate })
  const previewFolderAiSummary = usePreviewFolderAiSummary()
  const createFolderSummary = useCreateFolderSummary()
  const updateFolderSummary = useUpdateFolderSummary()

  const folderSummary = folderSummaryQuery.data ?? null

  const { progressDraft, planDraft, setProgressDraft, setPlanDraft, reset } =
    useFolderSummaryEditor(folderSummary)

  const totalCommitCount = reports.reduce((sum, r) => sum + r.commitCount, 0)
  const uniqueContributorCount = new Set(
    reports.flatMap(r => r.commits.map(c => c.authorEmail))
  ).size

  const handleSave = () => {
    if (folderSummary) {
      updateFolderSummary.mutate(
        {
          id: folderSummary.id,
          payload: {
            progressSummary: progressDraft,
            planSummary: planDraft,
          },
        },
        {
          onSuccess: () => setIsEditing(false),
          onError: (err: Error) => showToast(err.message),
        },
      )
    } else {
      createFolderSummary.mutate(
        {
          folderId,
          startDate,
          endDate,
          progressSummary: progressDraft,
          planSummary: planDraft,
        },
        {
          onSuccess: () => setIsEditing(false),
          onError: (err: Error) => showToast(err.message),
        },
      )
    }
  }

  const handleCancel = () => {
    reset()
    setIsEditing(false)
  }

  const handleGenerateAi = () => {
    previewFolderAiSummary.mutate(
      { folderId, startDate, endDate },
      {
        onSuccess: (preview) => {
          setProgressDraft(stripSectionHeader(preview.progressSummary))
          setPlanDraft(stripSectionHeader(preview.planSummary))
          setIsEditing(true)
          if (preview.aiSummaryFailed) showToast('AI 요약 생성에 실패하여 기본 요약으로 대체되었습니다.')
        },
        onError: (err: Error) => showToast(err.message),
      },
    )
  }

  const panelContent = isEditing ? (
    <div className={styles.summaryContent}>
      <div className={styles.summarySection}>
        <h4 className={styles.sectionTitle}>금주 진행사항</h4>
        <textarea
          className={styles.textarea}
          value={progressDraft}
          onChange={(e) => setProgressDraft(e.target.value)}
          placeholder="금주 진행사항을 입력하세요..."
          autoFocus
        />
      </div>
      <div className={styles.summarySection}>
        <h4 className={styles.sectionTitle}>차주 진행계획</h4>
        <textarea
          className={styles.textarea}
          value={planDraft}
          onChange={(e) => setPlanDraft(e.target.value)}
          placeholder="차주 진행계획을 입력하세요..."
        />
      </div>
    </div>
  ) : (
    <div className={styles.summaryContent}>
      {progressDraft || planDraft ? (
        <>
          <div className={styles.summarySection}>
            <h4 className={styles.sectionTitle}>금주 진행사항</h4>
            {(progressDraft || '').split('\n').map((line, i) => (
              <p key={i} className={styles.summaryLine}>{line}</p>
            ))}
          </div>
          <div className={styles.summarySection}>
            <h4 className={styles.sectionTitle}>차주 진행계획</h4>
            {(planDraft || '').split('\n').map((line, i) => (
              <p key={i} className={styles.summaryLine}>{line}</p>
            ))}
          </div>
        </>
      ) : (
        <span className={styles.summaryEmpty}>
          요약 내용이 없습니다.<br />
          AI 요약 또는 직접 편집해 주세요.
        </span>
      )}
    </div>
  )

  return (
    <>
      <Toast
        message={toast.message}
        visible={toast.visible}
        onClose={hideToast}
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
              <span className={styles.charCount}>{progressDraft.length + planDraft.length}자</span>
              <button
                className={styles.cancelBtn}
                onClick={handleCancel}
                disabled={updateFolderSummary.isPending || createFolderSummary.isPending}
              >
                취소
              </button>
              <button
                className={styles.saveBtn}
                onClick={handleSave}
                disabled={updateFolderSummary.isPending || createFolderSummary.isPending}
              >
                {(updateFolderSummary.isPending || createFolderSummary.isPending) ? '저장 중...' : '저장'}
              </button>
            </>
          ) : (
            <>
              <button
                className={styles.aiBtn}
                onClick={handleGenerateAi}
                disabled={previewFolderAiSummary.isPending}
              >
                {previewFolderAiSummary.isPending ? '생성 중...' : '✨ AI 요약'}
              </button>
              <button
                className={styles.editBtn}
                onClick={() => setIsEditing(true)}
                disabled={previewFolderAiSummary.isPending}
              >
                편집
              </button>
            </>
          )}
        </div>

        <div className={styles.panelContent}>
          {folderSummaryQuery.isLoading ? (
            <span className={styles.summaryEmpty}>불러오는 중...</span>
          ) : panelContent}
        </div>
      </aside>
    </>
  )
}
