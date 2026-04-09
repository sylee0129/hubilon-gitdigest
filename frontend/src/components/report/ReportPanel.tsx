import { useState, useEffect } from 'react'
import type { Report } from '../../types/report'
import { useGenerateAiSummary, useUpdateSummary } from '../../hooks/useReports'
import Toast from '../common/Toast'
import styles from './ReportPanel.module.css'

interface Props {
  report: Report | null
}

export default function ReportPanel({ report }: Props) {
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState(report?.summary ?? '')
  const [toastVisible, setToastVisible] = useState(false)
  const generateAiSummary = useGenerateAiSummary()
  const updateSummary = useUpdateSummary()

  useEffect(() => {
    setDraft(report?.summary ?? '')
    setIsEditing(false)
  }, [report?.id])

  const handleSave = () => {
    if (!report) return
    updateSummary.mutate(
      { id: report.id, payload: { summary: draft } },
      { onSuccess: () => setIsEditing(false) },
    )
  }

  const handleCancel = () => {
    setDraft(report?.summary ?? '')
    setIsEditing(false)
  }

  if (!report) {
    return (
      <aside className={styles.panel}>
        <div className={styles.emptyState}>
          <span className={styles.emptyIcon}>📄</span>
          <p>사이드바에서 프로젝트를 선택하면<br />보고서가 표시됩니다.</p>
        </div>
      </aside>
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
            <span className={styles.panelIcon}>📄</span>
            <span className={styles.panelTitleText}>보고서</span>
          </div>
          <span className={styles.panelProject}>{report.projectName}</span>
        </div>

        <div className={styles.panelActions}>
          {isEditing ? (
            <>
              <span className={styles.charCount}>{draft.length}자</span>
              <button
                className={styles.cancelBtn}
                onClick={handleCancel}
                disabled={updateSummary.isPending}
              >
                취소
              </button>
              <button
                className={styles.saveBtn}
                onClick={handleSave}
                disabled={updateSummary.isPending}
              >
                {updateSummary.isPending ? '저장 중...' : '저장'}
              </button>
            </>
          ) : (
            <>
              <button
                className={styles.aiBtn}
                onClick={() =>
                  generateAiSummary.mutate(report.id, {
                    onSuccess: (updatedReport: Report) => {
                      setDraft(updatedReport.summary ?? '')
                      setIsEditing(true)
                      if (updatedReport.aiSummaryFailed) {
                        setToastVisible(true)
                      }
                    },
                  })
                }
                disabled={generateAiSummary.isPending}
              >
                {generateAiSummary.isPending ? '생성 중...' : '✨ AI 요약'}
              </button>
              <button
                className={styles.editBtn}
                onClick={() => setIsEditing(true)}
              >
                편집
              </button>
            </>
          )}
        </div>

        <div className={styles.panelContent}>
          {isEditing ? (
            <textarea
              className={styles.textarea}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="보고서 내용을 입력하세요..."
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
