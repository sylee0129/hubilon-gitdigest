# 주간보고 Confluence 페이지 자동 생성 기능

## 개요

기존 주간보고 엑셀 다운로드와 동일한 데이터/구조로 Confluence 페이지를 자동 생성한다.
Header의 기존 엑셀 버튼 옆에 "Confluence 업로드" 버튼을 추가한다.

- **Confluence Space**: `Y2yJ6hFKUNFK`
- **Parent Page**: `주간보고`
- **페이지 제목 형식**: `4월_3주차_주간보고` (현재 선택된 주차 기준)
- **API**: Atlassian Confluence REST API v2 (백엔드 프록시 경유)

---

## 데이터 플로우

```
Header 버튼 클릭
  → useWeeklyConfluenceUpload (hook)
  → buildWeeklyReportRows(folders, startDate, endDate)  ← 공통 유틸
  → POST /api/confluence/weekly-report (백엔드)
    → Confluence REST API: GET /wiki/rest/api/content?spaceKey=&title=주간보고&type=page → parentId 조회
    → GET /wiki/rest/api/content?spaceKey=&title={pageTitle}&type=page → 기존 페이지 존재 여부 확인
    → 없으면 POST /wiki/rest/api/content (create)
    → 있으면 GET /wiki/rest/api/content/{id} → version.number 조회
             PUT /wiki/rest/api/content/{id} (update, version.number + 1)
  → 성공 시 Confluence 페이지 URL 반환 → 새 탭 오픈
```

---

## 백엔드 구현

### 1. 설정 추가 (`application.yml`)

```yaml
confluence:
  base-url: ${CONFLUENCE_BASE_URL:https://hubilon-platform-dev-team.atlassian.net}
  user-email: ${CONFLUENCE_USER_EMAIL:}
  api-token: ${CONFLUENCE_API_TOKEN:}
  space-key: ${CONFLUENCE_SPACE_KEY:Y2yJ6hFKUNFK}
  parent-page-title: ${CONFLUENCE_PARENT_PAGE_TITLE:주간보고}
```

환경변수 예시 (`.env.example`):
```
CONFLUENCE_BASE_URL=https://your-team.atlassian.net
CONFLUENCE_USER_EMAIL=your-email@company.com
CONFLUENCE_API_TOKEN=<Atlassian API Token>
CONFLUENCE_SPACE_KEY=YOUR_SPACE_KEY
CONFLUENCE_PARENT_PAGE_TITLE=주간보고
```

### 2. 새 모듈 구조

```
backend/src/main/java/com/hubilon/modules/confluence/
├── adapter/
│   ├── in/web/
│   │   ├── ConfluenceController.java          # POST /api/confluence/weekly-report
│   │   └── WeeklyConfluenceRequest.java       # 요청 DTO
│   └── out/external/
│       └── ConfluenceApiClient.java           # Confluence REST API 호출
├── application/
│   ├── port/in/
│   │   └── UploadWeeklyReportUseCase.java
│   └── service/
│       └── ConfluenceWeeklyReportService.java # 비즈니스 로직 + XHTML 생성
└── config/
    └── ConfluenceProperties.java              # @ConfigurationProperties
```

### 3. 요청/응답 DTO

**WeeklyConfluenceRequest**
```java
record WeeklyConfluenceRequest(
    List<WeeklyReportRowDto> rows,
    String startDate,   // YYYY-MM-DD
    String endDate      // YYYY-MM-DD
) {}

record WeeklyReportRowDto(
    String category,        // DEVELOPMENT | NEW_BUSINESS | OTHER
    String folderName,
    List<String> members,
    String progressSummary,
    String planSummary
) {}
```

**응답**: `{ "pageUrl": "https://...atlassian.net/wiki/..." }`

### 4. ConfluenceApiClient — 주요 호출

| 역할 | HTTP | 엔드포인트 |
|------|------|-----------|
| 상위 페이지 ID 조회 | GET | `/wiki/rest/api/content?spaceKey=&title=주간보고&type=page` |
| 동일 제목 페이지 조회 | GET | `/wiki/rest/api/content?spaceKey=&title={pageTitle}&type=page` |
| 페이지 버전 조회 | GET | `/wiki/rest/api/content/{id}` |
| 페이지 생성 | POST | `/wiki/rest/api/content` |
| 페이지 수정 | PUT | `/wiki/rest/api/content/{id}` |

인증: Basic Auth (`email:apiToken` → Base64)

### 5. XHTML 생성 — XSS 방지

`progressSummary`, `planSummary`, `folderName`, `members` 등 사용자 입력 데이터는
반드시 HTML 이스케이프 후 삽입한다:

```java
private String escape(String text) {
    if (text == null) return "";
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
}
```

줄바꿈 처리: `escape(text).replace("\n", "<br/>")` (이스케이프 후 줄바꿈 변환)

### 6. XHTML 테이블 구조 (Confluence Storage Format)

```xml
<h2>플랫폼개발실 | MM월 N주</h2>
<table>
  <colgroup>
    <col style="width: 8%;" />
    <col style="width: 14%;" />
    <col style="width: 35%;" />
    <col style="width: 35%;" />
    <col style="width: 8%;" />
  </colgroup>
  <tbody>
    <tr>
      <th style="background-color: #dae4f0; text-align: center;">사업구분</th>
      <th style="background-color: #dae4f0; text-align: center;">프로젝트명</th>
      <th style="background-color: #dae4f0; text-align: center;">금주 진행사항 (M.DD~M.DD)</th>
      <th style="background-color: #dae4f0; text-align: center;">차주 진행계획 (M.DD~M.DD)</th>
      <th style="background-color: #dae4f0; text-align: center;">담당자</th>
    </tr>
    <!-- 카테고리별 rowspan 적용, 모든 텍스트는 escape() 처리 -->
    <tr>
      <td rowspan="N" style="background-color: #dae4f0; text-align: center; font-weight: bold;">개발사업</td>
      <td>프로젝트명</td>
      <td>금주 진행사항</td>
      <td>차주 진행계획</td>
      <td style="text-align: center;">담당자1<br/>담당자2</td>
    </tr>
  </tbody>
</table>
```

---

## 프론트엔드 구현

### 1. 공통 유틸 추출 (`buildWeeklyReportRows.ts`)

`useWeeklyExcelDownload`와 `useWeeklyConfluenceUpload`의 중복 로직을 분리:

```typescript
// frontend/src/utils/buildWeeklyReportRows.ts
export async function buildWeeklyReportRows(
  folders: Folder[],
  startDate: string,
  endDate: string
): Promise<WeeklyReportRow[]>
```

`useWeeklyExcelDownload.ts`도 이 유틸을 사용하도록 리팩토링.

### 2. API 서비스 (`confluenceApi.ts`)

기존 `reportApi.ts`와 동일한 패턴으로 별도 파일로 분리
(Confluence는 report 도메인과 독립적이므로 분리가 적절):

```typescript
// frontend/src/services/confluenceApi.ts
export const confluenceApi = {
  uploadWeeklyReport: async (params: {
    rows: WeeklyReportRow[]
    startDate: string
    endDate: string
  }): Promise<{ pageUrl: string }> => {
    const res = await apiClient.post('/confluence/weekly-report', params)
    return res.data.data
  }
}
```

### 3. 훅 (`useWeeklyConfluenceUpload.ts`)

```typescript
// frontend/src/hooks/useWeeklyConfluenceUpload.ts
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

  return { upload, loading }
}
```

### 4. `Header.tsx` 버튼 추가

기존 엑셀 버튼 옆에 추가:
```tsx
<button onClick={upload} disabled={confluenceLoading || !folders?.length}>
  {confluenceLoading ? '업로드 중...' : 'Confluence 업로드'}
</button>
```

---

## 페이지 제목 계산 로직

`startDate` 기준, 백엔드에서 계산:

```
"2026-04-13" → 4월 3주차 → "4월_3주차_주간보고"
```

- 월: `date.getMonthValue()`
- 주차: `(date.getDayOfMonth() + firstDayOfMonth.getDayOfWeek().getValue() - 1 - 1) / 7 + 1`
- 형식: `${month}월_${week}주차_주간보고`

월 경계 처리: 계산된 주차가 실제 해당 월에 속하는지 검증 (예: 3월 31일이 4월 1주차에 걸칠 경우 startDate 월 기준으로 고정).

---

## 작업 순서

### Phase 1 — 백엔드
1. `ConfluenceProperties` + `application.yml` 설정 추가
2. `ConfluenceApiClient` 구현 (Basic Auth + REST 호출)
3. XHTML 생성 로직 구현 (`ConfluenceWeeklyReportService`) — HTML 이스케이프 포함
4. `ConfluenceController` + Use Case 구현
5. 환경변수 설정 후 로컬 테스트

### Phase 2 — 프론트엔드
1. `buildWeeklyReportRows.ts` 공통 유틸 추출 + `useWeeklyExcelDownload` 리팩토링
2. `confluenceApi.ts` 서비스 추가
3. `useWeeklyConfluenceUpload` 훅 구현
4. `Header.tsx` 버튼 추가

---

## 예외 처리

| 상황 | 처리 방법 |
|------|-----------|
| 동일 제목 페이지 이미 존재 | 버전 조회 후 +1 하여 PUT update |
| 상위 페이지 `주간보고` 없음 | 400 에러 + "상위 페이지를 찾을 수 없습니다" 메시지 |
| Confluence API 인증 실패 | 401 에러 + 서버 로그에 상세 기록 |
| 데이터 없음 (folders 빈 배열) | 업로드 버튼 disabled 처리 |

---

## Review 결과
- 검토일: 2026-04-13
- 검토 항목: 보안 / 리팩토링 / 기능
- 수정 내용:
  - [보안] 환경변수 예시에서 실제 이메일 제거 → placeholder로 교체
  - [보안] updatePage HTTP 메서드 오타 수정 (POST → PUT)
  - [보안] XHTML Injection 방지: 사용자 입력 HTML 이스케이프 처리 명시
  - [리팩토링] 데이터 조립 로직 `buildWeeklyReportRows()` 공통 유틸로 추출
  - [기능] update 플로우에 버전 조회 단계 추가 (GET → version.number → PUT)
  - [기능] 주차 계산 월 경계 엣지케이스 처리 명시
