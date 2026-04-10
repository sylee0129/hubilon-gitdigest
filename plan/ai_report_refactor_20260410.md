# AI 보고서 자동 생성 리팩토링 계획

## Context
현재 AI 요약(`AiSummaryAdapter`)은 단순 author별 1~2줄 텍스트를 생성함. 목표는 실무 엑셀 보고서 양식에 바로 붙여넣기 가능한 **금주 진행사항 / 차주 진행계획** 구조 출력으로 고도화. 두 섹션은 향후 엑셀 내보내기 시 각각 다른 셀에 매핑됨.

---

## 변경 범위 요약

- **개인 프로젝트 보고서** (ReportAiSummarizeService) → 변경 없음
- **폴더 보고서** (FolderSummaryAiSummarizeService) → 구조화된 2섹션 출력으로 변경

---

## Backend 변경

### 1. `AiSummaryResult` 타입 분리
`backend/.../report/domain/model/AiSummaryResult.java`

개인 보고서와 폴더 보고서의 반환 타입을 분리한다. 기존 `AiSummaryResult`는 개인 보고서용으로 유지하고, 폴더 전용 record를 신규 추가:

```java
// 기존 — 변경 없음
public record AiSummaryResult(String summary, boolean aiUsed) {}

// 신규
public record FolderAiSummaryResult(
    String progressSummary,  // 금주 진행사항
    String planSummary,      // 차주 진행계획
    boolean aiUsed
) {}
```

### 2. `AiSummaryPort` 오버로드 추가
`backend/.../report/domain/port/out/AiSummaryPort.java`

```java
public interface AiSummaryPort {
    // 기존 (개인 보고서, 변경 없음)
    AiSummaryResult summarize(List<CommitInfo> commits);

    // 신규 (폴더 보고서용 — 날짜 포함)
    FolderAiSummaryResult summarizeFolder(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate);
}
```

### 3. `AiSummaryAdapter` 수정
`backend/.../report/adapter/out/ai/AiSummaryAdapter.java`

#### 3-1. 공통 Gemini 호출 추상화
기존 `callLlmApi()`의 HTTP 호출·에러 처리·응답 파싱 로직을 내부 메서드로 분리:

```java
private String callGemini(String prompt) {
    // WebClient POST → 응답 텍스트 추출
    // 실패 시 null 반환 (fallback은 호출측에서 처리)
}
```

#### 3-2. AI 응답 텍스트 sanitize
`callGemini()` 반환값에 HTML 태그 제거 적용:
```java
private String sanitize(String text) {
    if (text == null) return null;
    return text.replaceAll("<[^>]*>", "").trim();
}
```

#### 3-3. `summarizeFolder()` 구현 — 구조화 프롬프트

```
당신은 IT 개발사업 주간보고서 작성 전문가입니다.
아래 Git 커밋 목록을 분석하여 주간보고서 요약문을 작성하세요.

작성 규칙:
- 문체: "~함", "~완료", "~진행 중" 으로 끝내는 짧고 명확한 문장
- 담당자명: 작업내용 형식으로 작성
- 차주 계획은 금주 미완료 사항 및 연속 작업으로 추론
- 설명 문구, 머리말, 꼬리말 없이 형식만 출력

출력 형식:
[금주 진행사항 ({startMD}~{endMD})]
- 담당자명: 작업내용 요약

[차주 진행계획 ({nextStartMD}~{nextEndMD})]
- 담당자명: 예정 작업 내용

[커밋 목록]
...
```

#### 3-4. 응답 파싱 + fallback 정책
- `[금주 진행사항` 구분자를 기준으로 `progressSummary` / `planSummary` 분리
- 파싱 실패(구분자 없음) → 전체 응답을 `progressSummary`에 담고 `planSummary`는 `"(자동 추론 불가)"` 로 설정
- 한 섹션만 파싱 성공 → 나머지는 위와 동일 기본값 적용
- stub fallback도 동일한 `FolderAiSummaryResult` 구조 반환

### 4. `FolderSummary` 도메인 모델
`backend/.../report/domain/model/FolderSummary.java`

필드 추가:
```java
private String progressSummary;  // 금주 진행사항
private String planSummary;      // 차주 진행계획
```
`withAiSummary()`, `withManualSummary()` 메서드도 두 필드 포함하도록 업데이트.

### 5. `FolderSummaryJpaEntity` 컬럼 추가
`backend/.../report/adapter/out/persistence/FolderSummaryJpaEntity.java`

```java
@Lob @Column(columnDefinition = "TEXT", name = "progress_summary")
private String progressSummary;

@Lob @Column(columnDefinition = "TEXT", name = "plan_summary")
private String planSummary;
```
`updateSummary()` 메서드도 두 필드 포함.

### 6. `FolderSummaryAiSummarizeService` 수정
`backend/.../report/application/service/FolderSummaryAiSummarizeService.java`

- 입력 검증: `command.startDate()`, `command.endDate()` null 체크 + `endDate >= startDate` 검증 → 위반 시 `InvalidRequestException`
- 기존 `aiSummaryPort.summarize(allCommits)` →
```java
FolderAiSummaryResult aiResult = aiSummaryPort.summarizeFolder(
    allCommits, command.startDate(), command.endDate());
```
- `FolderSummary` 빌드 시 `progressSummary`, `planSummary` 필드 추가

### 7. `FolderSummaryResult` DTO 확장
`backend/.../report/application/dto/FolderSummaryResult.java`

```java
public record FolderSummaryResult(
    ...,
    String progressSummary,
    String planSummary
) {}
```

### 8. `FolderSummaryResponse` / `FolderSummaryUpdateRequest` / 매퍼 업데이트
- `FolderSummaryResponse.java`: 두 필드 추가
- `FolderSummaryUpdateRequest.java`: 수동 편집용 두 필드 추가
  - 빈 문자열(`""`) 입력은 null로 변환하여 저장 (서비스 레이어에서 처리)
  - 두 필드 모두 nullable — 하나만 전송해도 나머지는 기존 값 유지 (PATCH 의미론)
- `FolderSummaryAppMapper.java`: 신규 필드 매핑
- `FolderSummaryUpdateService.java`: PATCH 처리 — null 필드는 기존 값 보존

---

## Frontend 변경

### 1. `types/report.ts`
```typescript
interface FolderSummary {
  ...
  progressSummary: string | null  // 금주 진행사항
  planSummary: string | null      // 차주 진행계획
}
```

### 2. `useFolderSummaryEditor` 커스텀 훅 (신규)
`frontend/src/hooks/useFolderSummaryEditor.ts`

`FolderReportPanel`의 편집 상태를 추상화. 두 draft 필드의 초기화·dirty 체크·저장 로직을 한 곳에서 관리:

```typescript
function useFolderSummaryEditor(folderSummary: FolderSummary | null) {
  // progressDraft, planDraft state
  // reset(), isDirty, handleSave() 등
}
```

### 3. `FolderReportPanel.tsx`
- draft 상태 관리를 `useFolderSummaryEditor` 훅으로 위임
- View 모드: 두 섹션을 구분 표시 ("금주 진행사항" / "차주 진행계획")
- Edit 모드: 두 개의 `<textarea>` (섹션별 독립 편집)
- 저장 시 `{ progressSummary: progressDraft ?? null, planSummary: planDraft ?? null }` 전송

### 4. `reportApi.ts` & `useReports.ts`
- `updateFolderSummary` payload 타입에 `progressSummary`, `planSummary` 포함

---

## 적용 순서 (의존성 순서)

1. Backend: `FolderAiSummaryResult` (신규) → `AiSummaryPort` → `AiSummaryAdapter`
2. Backend: `FolderSummary` (domain) → `FolderSummaryJpaEntity` (persistence)
3. Backend: `FolderSummaryResult` DTO → `FolderSummaryAiSummarizeService`
4. Backend: Response/Request record → `FolderSummaryUpdateService` → mapper → controller
5. Frontend: types → `useFolderSummaryEditor` 훅 → `FolderReportPanel` → api/hooks

---

## 검증 방법

1. `ai.summary.enabled=false` 상태에서 폴더 AI 요약 호출 → stub이 `progressSummary`/`planSummary` 구조로 반환되는지 확인
2. Gemini API 키 설정 후 폴더 AI 요약 호출 → 두 섹션이 DB에 분리 저장되는지 확인
3. AI 응답에 HTML 태그 포함 케이스 → sanitize 후 저장 확인
4. 섹션 하나만 수정 후 저장 → 나머지 필드 기존 값 유지 확인 (PATCH 동작)
5. Frontend에서 섹션별 편집 → 저장 → 재조회 시 각 필드 유지 확인
6. 기존 개인 프로젝트 AI 요약 (`POST /api/reports/{id}/ai-summary`) 동작 무변경 확인

---

## 영향받지 않는 파일
- `AiSummaryResult.java` (개인 보고서용 record — 변경 없음)
- `ReportAiSummarizeService.java` (개인 보고서 — summarize() 그대로 사용)
- `ReportExportService.java` (현재는 summary 필드만 사용 — 추후 별도 확장)
- `ReportPanel.tsx`, `ReportCard.tsx` (개인 보고서 UI — 변경 없음)

---

## Review 결과
- 검토일: 2026-04-10
- 검토 항목: 보안 / 리팩토링 / 기능
- 주요 반영 사항:
  - XSS 방어: `sanitize()` 메서드로 AI 응답 HTML 태그 제거
  - 타입 분리: `FolderAiSummaryResult` 신규 record로 혼재 해소
  - Gemini 호출 추상화: 내부 `callGemini(prompt)` 메서드 분리
  - 파싱 실패 fallback: 섹션 구분자 누락 시 기본값 정책 명시
  - PATCH 정책: null 필드는 기존 값 보존
  - 날짜 역전 검증: 서비스 레이어 입력 검증 추가
- 이번 범위 제외 항목 (추후): 재시도/서킷브레이커, 엑셀 셀 매핑 스펙
