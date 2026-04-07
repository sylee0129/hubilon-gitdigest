import { useState } from 'react'
import { useUpdateSummary } from '../../hooks/useReports'
import styles from './SummaryEditor.module.css'

interface Props {
  reportId: number
  isEditing: boolean
  currentSummary: string
  onEditToggle: () => void
}

export default function SummaryEditor({
  reportId,
  isEditing,
  currentSummary,
  onEditToggle,
}: Props) {
  const [draft, setDraft] = useState(currentSummary)
  const updateSummary = useUpdateSummary()

  const handleSave = () => {
    updateSummary.mutate(
      { id: reportId, payload: { summary: draft } },
      { onSuccess: onEditToggle },
    )
  }

  const handleCancel = () => {
    setDraft(currentSummary)
    onEditToggle()
  }

  if (!isEditing) {
    return (
      <div className={styles.viewMode}>
        {currentSummary
          ? currentSummary.split('\n').map((line, i) => (
              <p key={i} className={styles.summaryLine}>
                {line}
              </p>
            ))
          : <span className={styles.empty}>요약 내용이 없습니다.</span>}
      </div>
    )
  }

  return (
    <div className={styles.editMode}>
      <textarea
        className={styles.textarea}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        rows={6}
        placeholder="보고서 요약을 입력하세요..."
        autoFocus
      />
      {updateSummary.isError && (
        <div className={styles.errorMsg}>
          {updateSummary.error instanceof Error
            ? updateSummary.error.message
            : '저장에 실패했습니다.'}
        </div>
      )}
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
  )
}
