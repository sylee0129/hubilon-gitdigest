import { useState } from 'react'
import { useFolders } from './useFolders'
import { useReportStore } from '../stores/useReportStore'
import { useAuthStore } from '../stores/useAuthStore'
import { buildWeeklyReportRows } from '../utils/buildWeeklyReportRows'
import { confluenceApi } from '../services/confluenceApi'

export function useWeeklyConfluenceUpload() {
  const { startDate, endDate } = useReportStore()
  const { data: folders } = useFolders('IN_PROGRESS')
  const user = useAuthStore((s) => s.user)
  const [loading, setLoading] = useState(false)

  const upload = async () => {
    if (!folders || folders.length === 0) return
    if (!user?.teamId) {
      alert('소속 팀 정보가 없습니다. 관리자에게 문의하세요.')
      return
    }
    setLoading(true)
    try {
      const rows = await buildWeeklyReportRows(folders, startDate, endDate)
      const { pageUrl } = await confluenceApi.uploadWeeklyReport({
        teamId: user.teamId,
        rows,
        startDate,
        endDate,
      })
      window.open(pageUrl, '_blank')
    } finally {
      setLoading(false)
    }
  }

  return { upload, loading, disabled: !folders || folders.length === 0 }
}
