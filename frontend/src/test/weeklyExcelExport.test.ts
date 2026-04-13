// @ts-nocheck
import { describe, it, expect, vi, beforeEach } from 'vitest'

// vi.hoisted ensures refs are available inside vi.mock factory (hoisted to top)
const mocks = vi.hoisted(() => {
  const mockSheets = []
  const mockWriteBuffer = vi.fn().mockResolvedValue(new ArrayBuffer(0))

  function MockWorkbook() {
    this.xlsx = { writeBuffer: mockWriteBuffer }
    this.addWorksheet = function(name) {
      const cells = {}
      const rows = {}
      const sheet = {
        _name: name,
        columns: [],
        getCell: function(addr) {
          if (!cells[addr]) cells[addr] = { value: undefined, font: {}, alignment: {}, fill: {}, border: {} }
          return cells[addr]
        },
        getRow: function(n) {
          if (!rows[n]) rows[n] = { height: 0 }
          return rows[n]
        },
        mergeCells: function() {},
      }
      mockSheets.push(sheet)
      return sheet
    }
  }

  return { mockSheets, mockWriteBuffer, MockWorkbook }
})

vi.mock('exceljs', () => ({
  default: { Workbook: mocks.MockWorkbook },
}))

vi.mock('../types/folder', () => ({
  CATEGORY_LABELS: {
    DEVELOPMENT: '개발',
    NEW_BUSINESS: '신사업',
    OTHER: '기타',
  },
}))

Object.defineProperty(globalThis, 'URL', {
  value: {
    createObjectURL: vi.fn(() => 'blob:mock'),
    revokeObjectURL: vi.fn(),
  },
  writable: true,
})

import { exportWeeklyExcel } from '../utils/weeklyExcelExport'

function getLastSheet() {
  return mocks.mockSheets[mocks.mockSheets.length - 1]
}

function resetSheets() {
  mocks.mockSheets.length = 0
  mocks.mockWriteBuffer.mockResolvedValue(new ArrayBuffer(0))
}

describe('getWeekOfMonth — 주차 계산 로직', () => {
  beforeEach(resetSheets)

  it('정상 케이스: 2026-04-06 → 04월 2주', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-04-06', endDate: '2026-04-10' })
    expect(getLastSheet()._name).toBe('04월 2주')
  })

  it('정상 케이스: 2026-04-13 → 04월 3주', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-04-13', endDate: '2026-04-17' })
    expect(getLastSheet()._name).toBe('04월 3주')
  })

  it('월 경계 케이스: 2026-03-30 → 03월 5주 (3월 1일 일요일)', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-03-30', endDate: '2026-04-03' })
    expect(getLastSheet()._name).toBe('03월 5주')
  })

  it('월 1일은 항상 1주차', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-05-01', endDate: '2026-05-05' })
    expect(getLastSheet()._name).toBe('05월 1주')
  })

  it('파일명에 주차 레이블(공백 없음)이 포함된다', async () => {
    const mockAnchor = { href: '', download: '', click: vi.fn() }
    vi.spyOn(document, 'createElement').mockReturnValueOnce(mockAnchor)
    vi.spyOn(document.body, 'appendChild').mockImplementation((el) => el)
    vi.spyOn(document.body, 'removeChild').mockImplementation((el) => el)

    await exportWeeklyExcel({ rows: [], startDate: '2026-04-13', endDate: '2026-04-17' })

    expect(mockAnchor.download).toContain('04월3주')
    expect(mockAnchor.download).toContain('주간보고_플랫폼개발실')
    expect(mockAnchor.download).toMatch(/\.xlsx$/)
  })

  it('헤더 C3에 금주 날짜 범위가 포함된다', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-04-13', endDate: '2026-04-17' })
    const c3Value = getLastSheet().getCell('C3').value
    expect(c3Value).toContain('4.13')
    expect(c3Value).toContain('4.17')
  })
})

describe('exportWeeklyExcel — 데이터 행 렌더링', () => {
  beforeEach(resetSheets)

  it('카테고리 순서: DEVELOPMENT → NEW_BUSINESS → OTHER', async () => {
    await exportWeeklyExcel({
      rows: [
        { category: 'OTHER', folderName: '기타프로젝트', members: ['C'], progressSummary: '', planSummary: '' },
        { category: 'DEVELOPMENT', folderName: '개발프로젝트', members: ['A'], progressSummary: '', planSummary: '' },
        { category: 'NEW_BUSINESS', folderName: '신사업프로젝트', members: ['B'], progressSummary: '', planSummary: '' },
      ],
      startDate: '2026-04-13',
      endDate: '2026-04-17',
    })
    const sheet = getLastSheet()
    expect(sheet.getCell('B4').value).toBe('개발프로젝트')
    expect(sheet.getCell('B5').value).toBe('신사업프로젝트')
    expect(sheet.getCell('B6').value).toBe('기타프로젝트')
  })

  it('members 배열이 줄바꿈으로 합쳐진다 (E열)', async () => {
    await exportWeeklyExcel({
      rows: [{ category: 'DEVELOPMENT', folderName: '팀프로젝트', members: ['홍길동', '김철수'], progressSummary: '', planSummary: '' }],
      startDate: '2026-04-13',
      endDate: '2026-04-17',
    })
    expect(getLastSheet().getCell('E4').value).toBe('홍길동\n김철수')
  })

  it('C열이 금주 진행사항, D열이 차주 계획이다', async () => {
    await exportWeeklyExcel({
      rows: [{ category: 'DEVELOPMENT', folderName: '프로젝트', members: ['홍길동'], progressSummary: 'API 구현', planSummary: '테스트 예정' }],
      startDate: '2026-04-13',
      endDate: '2026-04-17',
    })
    const sheet = getLastSheet()
    expect(sheet.getCell('C4').value).toBe('API 구현')
    expect(sheet.getCell('D4').value).toBe('테스트 예정')
  })

  it('빈 rows일 때도 시트가 생성된다', async () => {
    await exportWeeklyExcel({ rows: [], startDate: '2026-04-13', endDate: '2026-04-17' })
    expect(mocks.mockSheets).toHaveLength(1)
    expect(getLastSheet()._name).toBe('04월 3주')
  })
})
