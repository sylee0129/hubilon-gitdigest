import { useState } from 'react'
import type { Report } from '../../types/report'
import CommitList from './CommitList'
import Toast from '../common/Toast'
import { useGenerateAiSummary, useUpdateSummary } from '../../hooks/useReports'
import styles from './ReportCard.module.css'

interface Props {
  report: Report
}

export default function ReportCard({ report }: Props) {
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState(report.summary ?? '')
  const [toastVisible, setToastVisible] = useState(false)
  const generateAiSummary = useGenerateAiSummary()
  const updateSummary = useUpdateSummary()

  const handleSave = () => {
    updateSummary.mutate(
      { id: report.id, payload: { summary: draft } },
      { onSuccess: () => setIsEditing(false) },
    )
  }

  const handleCancel = () => {
    setDraft(report.summary ?? '')
    setIsEditing(false)
  }

  return (
    <>
    <Toast
      message="AI 요약 생성에 실패하여 기본 요약으로 대체되었습니다."
      visible={toastVisible}
      onClose={() => setToastVisible(false)}
    />
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        <div className={styles.projectInfo}>
          <h3 className={styles.projectName}>{report.projectName}</h3>
          <span className={styles.dateRange}>
            {report.startDate} ~ {report.endDate}
          </span>
        </div>
        <div className={styles.stats}>
          <span className={styles.statItem}>
            커밋 <strong>{report.commitCount}건</strong>
          </span>
          <span className={styles.statDot}>·</span>
          <span className={styles.statItem}>
            기여자 <strong>{report.contributorCount}명</strong>
          </span>
        </div>
      </div>

      <div className={styles.divider} />

      <div className={styles.contentArea}>
        <div className={styles.leftPanel}>
          <CommitList commits={report.commits ?? []} />
        </div>

        <div className={styles.rightPanel}>
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
            {generateAiSummary.isPending ? '생성 중...' : '✨ AI 요약 생성'}
          </button>

          <div className={styles.reportArea}>
            {isEditing ? (
              <>
                <textarea
                  className={styles.textarea}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  rows={14}
                  placeholder="보고서 내용을 입력하세요..."
                  autoFocus
                />
                <div className={styles.textareaFooter}>
                  <span className={styles.charCount}>{draft.length}자</span>
                  <div className={styles.editActions}>
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
                  </div>
                </div>
              </>
            ) : (
              <div className={styles.viewMode}>
                <button
                  className={styles.editIconBtn}
                  onClick={() => setIsEditing(true)}
                  title="편집"
                >
                  ✏️
                </button>
                {draft
                  ? draft.split('\n').map((line, i) => (
                      <p key={i} className={styles.summaryLine}>
                        {line}
                      </p>
                    ))
                  : <span className={styles.summaryEmpty}>요약 내용이 없습니다.</span>}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
    </>
  )
}
