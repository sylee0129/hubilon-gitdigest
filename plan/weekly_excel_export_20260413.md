# 주간보고서 엑셀 다운로드 기능 구현 계획

## Context
경영진·실장님 보고용 전문 엑셀 주간보고서 다운로드 기능.
현재 엑셀 관련 라이브러리 미설치, 다운로드 기능 없음.
폴더별 FolderSummary(progressSummary, planSummary)와 Folder.members를 조합해 시트 생성.

---

## 데이터 흐름

```
useFolders() → Folder[] (name, category, members, workProjects)
         ↓
Promise.all → reportApi.getFolderSummary({ folderId, startDate, endDate })
         ↓
FolderSummaryResponse[] (progressSummary, planSummary)
         ↓
exportWeeklyExcel() → Blob → 다운로드
```

---

## 엑셀 레이아웃

| 행 | 내용 |
|----|------|
| 1행 | `주간 프로젝트 진행 현황` (전체 컬럼 병합, 타이틀) |
| 2행 | 부제: `플랫폼개발실 \| MM월 N주` |
| 3행 | 컬럼 헤더: 사업구분 / 프로젝트명 / 담당자 / 금주 진행사항 (M.DD~M.DD) / 차주 진행계획 (M.DD~M.DD) |
| 4행~ | 데이터 (카테고리 순: DEVELOPMENT → NEW_BUSINESS → OTHER) |

**컬럼 너비**
- A (사업구분): 15
- B (프로젝트명): 25
- C (담당자): 20
- D (금주 진행사항): 50
- E (차주 진행계획): 50

**스타일**
- 1행 타이틀: 배경 `#1F4E79`, 흰색 폰트, 16pt Bold, 가운데 정렬
- 2행 부제: 배경 `#2E75B6`, 흰색 폰트, 10pt, 가운데 정렬
- 3행 헤더: 배경 `#4472C4`, 흰색 폰트, 11pt Bold, 가운데 정렬, 테두리
- 데이터 행: 홀짝 배경 교차(`#FFFFFF` / `#EBF3FB`), WrapText, 테두리
- 사업구분 컬럼: 동일 카테고리 셀 병합(Merge), 배경 `#D6E4F0`, Bold

**시트명**: `MM월 N주` (예: `04월 2주`)  
→ startDate 기준으로 해당 월의 몇 번째 주인지 계산

**파일명**: `MM월N주_주간보고_플랫폼개발실_YYYYMMDD.xlsx`  
→ 예: `04월2주_주간보고_플랫폼개발실_20260413.xlsx`

---

## 담당자 표시

`folder.members` (FolderMember[]) → `name` 필드를 줄바꿈(`\n`)으로 조합  
데이터 없으면 빈 문자열.

## 진행사항 fallback

- progressSummary 없음 → `'진행사항 없음'`
- planSummary 없음 → `'진행사항 확인'`

---

## 구현 파일 목록

### 신규 생성

| 파일 | 역할 |
|------|------|
| `frontend/src/utils/weeklyExcelExport.ts` | exceljs 기반 워크북 생성 순수 함수 |
| `frontend/src/hooks/useWeeklyExcelDownload.ts` | 데이터 수집 + 다운로드 트리거 훅 |

### 수정

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/package.json` | `exceljs` 패키지 추가 (`npm install exceljs`) |
| `frontend/src/components/layout/Header.tsx` | Right 영역에 "주간보고 다운로드" 버튼 추가 |

---

## 주요 구현 상세

### weeklyExcelExport.ts 인터페이스

```typescript
interface WeeklyReportRow {
  category: 'DEVELOPMENT' | 'NEW_BUSINESS' | 'OTHER'
  folderName: string
  members: string[]          // FolderMember[].name
  progressSummary: string    // FolderSummaryResponse.progressSummary
  planSummary: string        // FolderSummaryResponse.planSummary
}

interface WeeklyExportParams {
  rows: WeeklyReportRow[]
  startDate: string   // YYYY-MM-DD
  endDate: string     // YYYY-MM-DD
}

export async function exportWeeklyExcel(params: WeeklyExportParams): Promise<void>
```

### useWeeklyExcelDownload.ts 로직

```typescript
export function useWeeklyExcelDownload() {
  const { startDate, endDate } = useReportStore()
  const { data: folders } = useFolders()
  const [loading, setLoading] = useState(false)

  const download = async () => {
    setLoading(true)
    // 1. 모든 폴더의 FolderSummary 병렬 조회 (Promise.all)
    // 2. WeeklyReportRow[] 구성 (카테고리 순 정렬)
    // 3. exportWeeklyExcel() 호출 → 파일 다운로드
    setLoading(false)
  }

  return { download, loading }
}
```

### 주차 계산 (시트명/파일명용)

```typescript
function getWeekOfMonth(dateStr: string): number {
  const date = new Date(dateStr)
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1)
  const firstMonday = /* 해당 월 첫 번째 월요일 */
  return Math.ceil((date.getDate() - firstMonday.getDate() + 1) / 7)
}
```

---

## 설치 명령

```bash
cd frontend && npm install exceljs
```

---

## 검증 방법

1. `npm install exceljs` 완료 및 빌드 오류 없음 확인
2. 헤더 우측 "주간보고 다운로드" 버튼 클릭
3. 로딩 표시 → 완료 시 파일 자동 다운로드 확인
4. 파일명 형식 확인: `04월2주_주간보고_플랫폼개발실_20260413.xlsx`
5. 엑셀 열기:
   - 시트명 확인 (`04월 2주`)
   - 1~3행 스타일 확인
   - 카테고리별 셀 병합 확인
   - progressSummary / planSummary 내용 확인
   - 데이터 없는 폴더 → fallback 문자열 확인
   - WrapText 적용 확인

---

## Review 결과
- 검토일: 2026-04-13
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 및 정책 결정 완료

### 결정 사항
1. **useFolders status 필터**: `IN_PROGRESS` 폴더만 포함
2. **FolderSummary null 처리**: 행 유지 — progressSummary/planSummary 모두 fallback 문자열 사용
3. **Promise.allSettled 적용**: 실패 폴더 skip 처리 (전체 다운로드 실패 방지)
4. **exceljs 호환성**: Vite 번들 검증 후 문제 시 xlsx(SheetJS) 대체
5. **날짜 유틸**: weeklyExcelExport.ts 내 로컬 구현 (기존 중복 패턴 유지)
6. **주차 계산 월 경계**: startDate 기준 월로 판단 (3/31이면 3월 N주)
