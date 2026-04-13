import { useState } from 'react'
import { useFolders } from './useFolders'
import { useReportStore } from '../stores/useReportStore'
import { reportApi } from '../services/reportApi'
import { exportWeeklyExcel, type WeeklyReportRow } from '../utils/weeklyExcelExport'
import type { Folder } from '../types/folder'

const CATEGORY_ORDER: Folder['category'][] = ['DEVELOPMENT', 'NEW_BUSINESS', 'OTHER']

export function useWeeklyExcelDownload() {
  const { startDate, endDate } = useReportStore()
  const { data: folders } = useFolders('IN_PROGRESS')
  const [loading, setLoading] = useState(false)

  const download = async () => {
    if (!folders || folders.length === 0) return

    setLoading(true)
    try {
      const results = await Promise.allSettled(
        folders.map((folder) =>
          reportApi.getFolderSummary({ folderId: folder.id, startDate, endDate })
        )
      )

      const rows: WeeklyReportRow[] = []

      results.forEach((result, i) => {
        if (result.status === 'rejected') return

        const folder = folders[i]
        const summary = result.value

        rows.push({
          category: folder.category,
          folderName: folder.name,
          members: folder.members.map((m) => m.name),
          progressSummary: summary?.progressSummary ?? '진행사항 없음',
          planSummary: summary?.planSummary ?? '진행사항 확인',
        })
      })

      // DEVELOPMENT → NEW_BUSINESS → OTHER 순 정렬
      rows.sort((a, b) => CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category))

      await exportWeeklyExcel({ rows, startDate, endDate })
    } finally {
      setLoading(false)
    }
  }

  return { download, loading }
}
