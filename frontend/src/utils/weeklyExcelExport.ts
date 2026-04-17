import ExcelJS from 'exceljs'

export interface WeeklyReportRow {
  categoryId: number
  categoryName: string
  folderName: string
  members: string[]
  progressSummary: string
  planSummary: string
}

export interface WeeklyExportParams {
  rows: WeeklyReportRow[]
  startDate: string  // YYYY-MM-DD
  endDate: string    // YYYY-MM-DD
}

function parseDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function getWeekOfMonth(dateStr: string): number {
  const date = parseDate(dateStr)
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1)
  return Math.ceil((date.getDate() + firstDay.getDay()) / 7)
}

function formatWeekLabel(dateStr: string): string {
  const date = parseDate(dateStr)
  const month = date.getMonth() + 1
  const week = getWeekOfMonth(dateStr)
  return `${String(month).padStart(2, '0')}월 ${week}주`
}

function formatWeekLabelNoSpace(dateStr: string): string {
  const date = parseDate(dateStr)
  const month = date.getMonth() + 1
  const week = getWeekOfMonth(dateStr)
  return `${String(month).padStart(2, '0')}월${week}주`
}

function formatMDD(dateStr: string): string {
  const date = parseDate(dateStr)
  const m = date.getMonth() + 1
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}.${d}`
}

function formatYYYYMMDD(dateStr: string): string {
  return dateStr.replace(/-/g, '')
}

// Style constants
const FONT_BASE: Partial<ExcelJS.Font> = { name: '맑은 고딕', size: 10 }
const FONT_BOLD: Partial<ExcelJS.Font> = { ...FONT_BASE, bold: true }
const FONT_TITLE: Partial<ExcelJS.Font> = { name: '맑은 고딕', size: 13, bold: true }

const BORDER_THIN: Partial<ExcelJS.Borders> = {
  top: { style: 'thin', color: { argb: 'FF000000' } },
  left: { style: 'thin', color: { argb: 'FF000000' } },
  bottom: { style: 'thin', color: { argb: 'FF000000' } },
  right: { style: 'thin', color: { argb: 'FF000000' } },
}

const BORDER_MEDIUM: Partial<ExcelJS.Borders> = {
  top: { style: 'medium', color: { argb: 'FF000000' } },
  left: { style: 'medium', color: { argb: 'FF000000' } },
  bottom: { style: 'medium', color: { argb: 'FF000000' } },
  right: { style: 'medium', color: { argb: 'FF000000' } },
}

const FILL_WHITE: ExcelJS.Fill = {
  type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFFFFF' }
}

const FILL_HEADER: ExcelJS.Fill = {
  type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFDAE4F0' }
}

export async function exportWeeklyExcel(params: WeeklyExportParams): Promise<void> {
  const { rows, startDate, endDate } = params

  const sortedRows = [...rows].sort((a, b) => a.categoryId - b.categoryId)

  const weekLabel = formatWeekLabel(startDate)
  const weekLabelNoSpace = formatWeekLabelNoSpace(startDate)
  const startMDD = formatMDD(startDate)
  const endMDD = formatMDD(endDate)
  const todayStr = formatYYYYMMDD(new Date().toISOString().split('T')[0])

  // 다음 주 날짜 계산
  const startDateObj = parseDate(startDate)
  const endDateObj = parseDate(endDate)
  const nextStartDateObj = new Date(startDateObj)
  nextStartDateObj.setDate(nextStartDateObj.getDate() + 7)
  const nextEndDateObj = new Date(endDateObj)
  nextEndDateObj.setDate(nextEndDateObj.getDate() + 7)
  const nextStartMDD = formatMDD(
    `${nextStartDateObj.getFullYear()}-${String(nextStartDateObj.getMonth() + 1).padStart(2, '0')}-${String(nextStartDateObj.getDate()).padStart(2, '0')}`
  )
  const nextEndMDD = formatMDD(
    `${nextEndDateObj.getFullYear()}-${String(nextEndDateObj.getMonth() + 1).padStart(2, '0')}-${String(nextEndDateObj.getDate()).padStart(2, '0')}`
  )

  const workbook = new ExcelJS.Workbook()
  const sheet = workbook.addWorksheet(weekLabel)

  // 열 너비
  sheet.columns = [
    { width: 8 },  // A: 사업구분
    { width: 14 }, // B: 프로젝트명
    { width: 55 }, // C: 금주 진행사항
    { width: 55 }, // D: 차주 진행계획
    { width: 12 }, // E: 담당자
  ]

  // --- 1행: 타이틀 (A1:E1 병합) ---
  sheet.getRow(1).height = 22
  sheet.mergeCells('A1:E1')
  const titleCell = sheet.getCell('A1')
  titleCell.value = `플랫폼개발팀 | ${weekLabel}`
  titleCell.font = FONT_TITLE
  titleCell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
  titleCell.fill = FILL_WHITE
  titleCell.border = BORDER_MEDIUM

  // --- 2행: spacer ---
  sheet.getRow(2).height = 4

  // --- 3행: 헤더 ---
  sheet.getRow(3).height = 20
  const headers = [
    '사업구분',
    '프로젝트명',
    `금주 진행사항 (${startMDD}~${endMDD})`,
    `차주 진행계획 (${nextStartMDD}~${nextEndMDD})`,
    '담당자',
  ]
  const headerCols = ['A', 'B', 'C', 'D', 'E']
  headers.forEach((h, i) => {
    const cell = sheet.getCell(`${headerCols[i]}3`)
    cell.value = h
    cell.font = FONT_BOLD
    cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
    cell.fill = FILL_HEADER
    cell.border = BORDER_MEDIUM
  })

  // --- 4행~: 데이터 ---
  // 카테고리별 병합 범위 계산
  type CategoryGroup = { categoryId: number; categoryName: string; startRow: number; endRow: number }
  const categoryGroups: CategoryGroup[] = []
  let groupStart = 4
  let prevCategoryId: number | null = null
  sortedRows.forEach((row, i) => {
    if (row.categoryId !== prevCategoryId) {
      if (prevCategoryId !== null) {
        const prevRow = sortedRows[i - 1]
        categoryGroups.push({ categoryId: prevCategoryId, categoryName: prevRow.categoryName, startRow: groupStart, endRow: 4 + i - 1 })
      }
      groupStart = 4 + i
      prevCategoryId = row.categoryId
    }
    if (i === sortedRows.length - 1) {
      categoryGroups.push({ categoryId: row.categoryId, categoryName: row.categoryName, startRow: groupStart, endRow: 4 + i })
    }
  })

  sortedRows.forEach((row, i) => {
    const excelRow = 4 + i
    sheet.getRow(excelRow).height = 60

    // B: 프로젝트명
    const bCell = sheet.getCell(`B${excelRow}`)
    bCell.value = row.folderName
    bCell.font = FONT_BASE
    bCell.alignment = { horizontal: 'left', vertical: 'top', wrapText: true }
    bCell.fill = FILL_WHITE
    bCell.border = BORDER_THIN

    // C: 금주 진행사항
    const cCell = sheet.getCell(`C${excelRow}`)
    cCell.value = row.progressSummary
    cCell.font = FONT_BASE
    cCell.alignment = { horizontal: 'left', vertical: 'top', wrapText: true }
    cCell.fill = FILL_WHITE
    cCell.border = BORDER_THIN

    // D: 차주 진행계획
    const dCell = sheet.getCell(`D${excelRow}`)
    dCell.value = row.planSummary
    dCell.font = FONT_BASE
    dCell.alignment = { horizontal: 'left', vertical: 'top', wrapText: true }
    dCell.fill = FILL_WHITE
    dCell.border = BORDER_THIN

    // E: 담당자
    const eCell = sheet.getCell(`E${excelRow}`)
    eCell.value = row.members.join('\n')
    eCell.font = FONT_BASE
    eCell.alignment = { horizontal: 'center', vertical: 'top', wrapText: true }
    eCell.fill = FILL_WHITE
    eCell.border = BORDER_THIN
  })

  // A열: 카테고리 (병합 후 스타일)
  categoryGroups.forEach((group) => {
    if (group.startRow === group.endRow) {
      // 단일 행 — 병합 없이 스타일만
      const cell = sheet.getCell(`A${group.startRow}`)
      cell.value = group.categoryName
      cell.font = FONT_BOLD
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
      cell.fill = FILL_HEADER
      cell.border = BORDER_THIN
    } else {
      // 복수 행 병합
      sheet.mergeCells(`A${group.startRow}:A${group.endRow}`)
      const cell = sheet.getCell(`A${group.startRow}`)
      cell.value = group.categoryName
      cell.font = FONT_BOLD
      cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
      cell.fill = FILL_HEADER
      cell.border = BORDER_THIN
    }
  })

  // 파일 저장 (브라우저)
  const buffer = await workbook.xlsx.writeBuffer()
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${weekLabelNoSpace}_주간보고_플랫폼개발팀_${todayStr}.xlsx`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
