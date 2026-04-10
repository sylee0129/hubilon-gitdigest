import { useState, useEffect } from 'react'
import type { FolderSummary } from '../types/report'

interface UseFolderSummaryEditorReturn {
  progressDraft: string
  planDraft: string
  setProgressDraft: (v: string) => void
  setPlanDraft: (v: string) => void
  reset: () => void
  isDirty: boolean
}

export function useFolderSummaryEditor(
  folderSummary: FolderSummary | null,
): UseFolderSummaryEditorReturn {
  const [progressDraft, setProgressDraft] = useState(folderSummary?.progressSummary ?? '')
  const [planDraft, setPlanDraft] = useState(folderSummary?.planSummary ?? '')

  const reset = () => {
    setProgressDraft(folderSummary?.progressSummary ?? '')
    setPlanDraft(folderSummary?.planSummary ?? '')
  }

  useEffect(() => {
    reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderSummary?.id])

  const isDirty =
    progressDraft !== (folderSummary?.progressSummary ?? '') ||
    planDraft !== (folderSummary?.planSummary ?? '')

  return { progressDraft, planDraft, setProgressDraft, setPlanDraft, reset, isDirty }
}
