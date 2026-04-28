# 스케줄러 AI 요약 미실행 원인 분석

## Context

스케줄러 실행 시 주간보고 내용(커밋)이 없으면 AI 요약이 실행되지 않는다고 보고됨.

---

## 원인 1 (확정): `ai.summary.enabled=false`

`application-dev.yml`에서 AI가 비활성화되어 있었음.

```yaml
ai:
  summary:
    enabled: false   # ← Gemini API 호출 안 됨
```

`AiSummaryAdapter.summarizeFolder()` 내부:
```java
if (aiEnabled) {
    return callFolderLlmApi(commits, ...);  // Gemini 호출
} else {
    return stubSummarizeFolder(commits, folderName);  // 커밋 목록만 나열
}
```

→ **이미 수정 완료** (`enabled: true`로 변경, master 반영)

---

## 원인 2 (버그): 커밋 없을 때 placeholder가 "정상 요약"으로 저장됨

`AiSummaryAdapter.summarizeFolder()`:
```java
if (commits == null || commits.isEmpty()) {
    return new FolderAiSummaryResult(
        "진행사항 없음",   // progressSummary
        "진행사항 확인",   // planSummary
        true               // aiUsed=true ← 실제 Gemini 호출 안 했지만 true
    );
}
```

`aiUsed=true` → `aiSummaryFailed = !aiUsed = false`로 저장됨.

결과적으로 DB에:
- `progressSummary = "진행사항 없음"`
- `planSummary = "진행사항 확인"`
- `aiSummaryFailed = false`

다음 스케줄러 실행 시 `WeeklyReportProcessor.process()`:
```java
boolean hasManualContent = existing != null
    && hasContent("진행사항 없음")   // true
    && hasContent("진행사항 확인");  // true
// → hasManualContent = true → Manual path → AI 완전 스킵
```

**결론**: 커밋 없음 → placeholder 저장 → 이후 실행에서 AI 재실행 안 됨.

---

## 수정 방향

### 방안 A: `AiSummaryAdapter` 커밋 없을 때 빈 값 반환

```java
if (commits == null || commits.isEmpty()) {
    return new FolderAiSummaryResult(
        "",     // 빈 값 → hasContent() = false → 다음 실행에서 AI 재시도
        "",
        false   // aiUsed=false → aiSummaryFailed=true → 명시적 실패 표시
    );
}
```

### 방안 B: `WeeklyReportProcessor`에서 `aiSummaryFailed=true`인 경우 AI 재시도

```java
boolean hasManualContent = existing != null
    && !existing.isAiSummaryFailed()   // AI 실패 기록 있으면 재시도
    && hasContent(existing.getProgressSummary())
    && hasContent(existing.getPlanSummary());
```

---

## 수정 파일

- `backend/.../report/adapter/out/ai/AiSummaryAdapter.java`
- `backend/.../scheduler/application/service/WeeklyReportProcessor.java`
