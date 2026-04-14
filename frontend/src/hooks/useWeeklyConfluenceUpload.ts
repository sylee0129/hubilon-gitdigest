import { useState } from 'react'
import { useFolders } from './useFolders'
import { useReportStore } from '../stores/useReportStore'
import { buildWeeklyReportRows } from '../utils/buildWeeklyReportRows'
import { confluenceApi } from '../services/confluenceApi'

export function useWeeklyConfluenceUpload() {
  const { startDate, endDate } = useReportStore()
  const { data: folders } = useFolders('IN_PROGRESS')
  const [loading, setLoading] = useState(false)

  const upload = async () => {
    if (!folders || folders.length === 0) return
    setLoading(true)
    try {
      const rows = await buildWeeklyReportRows(folders, startDate, endDate)
      const { pageUrl } = await confluenceApi.uploadWeeklyReport({ rows, startDate, endDate })
      window.open(pageUrl, '_blank')
    } finally {
      setLoading(false)
    }
  }

  return { upload, loading, disabled: !folders || folders.length === 0 }
}
