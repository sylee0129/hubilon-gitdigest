import type { Folder } from '../types/folder'
import { reportApi } from '../services/reportApi'
import type { WeeklyReportRow } from './weeklyExcelExport'

export async function buildWeeklyReportRows(
  folders: Folder[],
  startDate: string,
  endDate: string
): Promise<WeeklyReportRow[]> {
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
      categoryId: folder.categoryId,
      categoryName: folder.categoryName,
      folderName: folder.name,
      members: folder.members.map((m) => m.name),
      progressSummary: summary?.progressSummary ?? '진행사항 없음',
      planSummary: summary?.planSummary ?? '진행사항 확인',
    })
  })

  rows.sort((a, b) => a.categoryId - b.categoryId)
  return rows
}
