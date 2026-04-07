# ReportCard 레이아웃 개선 계획

## 요청 사항
1. summaryHeader 삭제
2. 커밋 이력 오른쪽에 보고서 작성 영역 생성
3. 보고서 작성 영역 위쪽으로 'AI 요약 생성' 버튼 이동
4. 'AI 요약 생성' 버튼 클릭 시, 결과가 보고서 내용에 입력됨

---

## 현재 구조 분석

### ReportCard.tsx 현재 레이아웃
```
card
├── cardHeader (프로젝트명, 날짜, 통계)
├── divider
├── summarySection
│   └── summaryHeader  ← 삭제 대상
│       └── AI 요약 생성 버튼
├── divider
└── CommitList (커밋 이력)
```

### 문제점
- `SummaryEditor` 컴포넌트가 `ReportCard`에 통합되지 않아 보고서 작성 영역 미노출
- AI 요약 생성 후 결과가 UI에 반영되지 않음 (쿼리 refetch만 발생)
- 커밋 이력과 보고서 작성 영역이 세로로 나열되어 공간 활용 비효율

---

## 목표 레이아웃

```
card
├── cardHeader (프로젝트명, 날짜, 통계)
├── divider
└── contentArea  ← 가로 2단 레이아웃
    ├── leftPanel (커밋 이력)
    │   └── CommitList
    └── rightPanel (보고서 작성)
        ├── AI 요약 생성 버튼  ← 상단 배치
        └── SummaryEditor (보고서 textarea + 저장/취소)
```

---

## 구현 계획

### 1. ReportCard.tsx 수정

#### 상태 추가
```tsx
const [isEditing, setIsEditing] = useState(false)
const [draft, setDraft] = useState(report.summary ?? '')
```

#### AI 요약 생성 onSuccess 처리
- `generateAiSummary.mutate` 의 `onSuccess` 콜백에서 생성된 요약을 `draft` 에 반영
- 현재 API 응답(`useGenerateAiSummary`)이 업데이트된 `Report`를 반환하므로 `summary` 필드로 draft 업데이트
- AI 생성 후 자동으로 편집 모드로 전환

```tsx
generateAiSummary.mutate(report.id, {
  onSuccess: (updatedReport) => {
    setDraft(updatedReport.summary ?? '')
    setIsEditing(true)
  }
})
```

#### 레이아웃 변경
- `summarySection` + `summaryHeader` + 하단 `divider` 제거
- `contentArea` div로 CommitList + 보고서 작성 패널을 가로 배치
- 보고서 작성 패널 내부: AI 버튼 → SummaryEditor 순서

#### SummaryEditor 통합
- `SummaryEditor`를 `ReportCard` 내에서 직접 인라인 구현 (props drilling 최소화)
- `draft`, `setDraft`, `isEditing`, `setIsEditing` 상태를 ReportCard에서 관리
- `useUpdateSummary` 훅도 ReportCard로 이동

### 2. ReportCard.module.css 수정

#### 삭제할 스타일
- `.summarySection`, `.summaryHeader`, `.summaryLabel`

#### 추가할 스타일
```css
/* 가로 2단 레이아웃 */
.contentArea {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.leftPanel {
  flex: 1;
  min-width: 0; /* 오버플로 방지 */
}

.rightPanel {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 반응형: 화면 좁을 때 세로 배치 */
@media (max-width: 900px) {
  .contentArea {
    flex-direction: column;
  }
  .rightPanel {
    width: 100%;
  }
}
```

---

## AI API 응답 타입 (확인 완료)

`reportApi.generateAiSummary(id): Promise<Report>` — 업데이트된 Report 객체 반환
- `onSuccess: (updatedReport: Report)` 콜백에서 `updatedReport.summary`로 draft 바로 업데이트 가능
- 별도 `useEffect` 불필요

---

## 개선 제안 (추가 구현 권장)

### A. 편집 모드 진입 버튼
- 보고서 작성 영역 우상단에 `✏️ 편집` 버튼 추가
- 뷰 모드 ↔ 편집 모드 토글

### B. 글자 수 표시
- textarea 하단에 현재 글자 수 표시 (예: `142자`)

### C. AI 생성 후 편집 모드 자동 진입
- AI 요약 생성 완료 시 자동으로 편집 모드로 전환
- 사용자가 내용 수정 후 저장 가능

### D. 미저장 경고
- 편집 중 페이지 이탈 또는 다른 탭 전환 시 미저장 경고 (추후 구현)

---

## 수정 대상 파일

| 파일 | 변경 내용 |
|------|---------|
| `frontend/src/components/report/ReportCard.tsx` | 레이아웃 재구성, AI 버튼 이동, SummaryEditor 통합 |
| `frontend/src/components/report/ReportCard.module.css` | summaryHeader 제거, 2단 레이아웃 스타일 추가 |
| `frontend/src/hooks/useReports.ts` | AI 요약 응답 타입 확인 (필요시 수정) |

---

## 검증 방법

1. `npm run dev` 로 프론트엔드 실행
2. 보고서 대시보드 진입
3. ReportCard에서 커밋 이력이 왼쪽, 보고서 작성 영역이 오른쪽에 위치하는지 확인
4. 'AI 요약 생성' 버튼이 보고서 작성 영역 상단에 위치하는지 확인
5. AI 버튼 클릭 시 생성된 내용이 textarea에 입력되는지 확인
6. 보고서 직접 편집 후 저장 동작 확인
7. 화면 폭 900px 미만에서 세로 배치로 전환되는지 확인
