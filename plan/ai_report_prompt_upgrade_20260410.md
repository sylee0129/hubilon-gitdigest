# AI 보고서 프롬프트 고도화 계획 (v2)

## Context
이전 구현(`ai_report_refactor_20260410.md`)에서 `progressSummary`/`planSummary` 2섹션 구조와 DB/API/프론트엔드를 완성함.
이번 작업은 **Gemini 프롬프트만** 교체하는 것이 핵심 — DB 스키마·API 스펙·프론트엔드 변경 없음.

목표: 샘플 엑셀(`03월2주_주간보고_플랫폼개발실_20260313.xlsx`)의 작성 스타일과 완벽히 일치하는 실무 보고서 수준 출력 확보.

---

## 변경 범위

**백엔드 단일 파일만 수정**:
- `AiSummaryAdapter.java` — `summarizeFolder()` 프롬프트 교체 + 폴더명 파라미터 추가
- `AiSummaryPort.java` — `summarizeFolder()` 시그니처에 `folderName` 파라미터 추가
- `FolderSummaryAiSummarizeService.java` — `folderName` 전달

**변경 없음**: DB, FolderSummary 모델, DTO, 프론트엔드 전체

---

## Backend 변경 상세

### 1. `AiSummaryPort` 시그니처 수정
`backend/.../report/domain/port/out/AiSummaryPort.java`

```java
// 변경 전
FolderAiSummaryResult summarizeFolder(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate);

// 변경 후 — folderName 추가
FolderAiSummaryResult summarizeFolder(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate, String folderName);
```

### 2. `FolderSummaryAiSummarizeService` 수정
`backend/.../report/application/service/FolderSummaryAiSummarizeService.java`

`summarizeFolder()` 호출 시 `folderName` 추가:
```java
FolderAiSummaryResult aiResult = aiSummaryPort.summarizeFolder(
    allCommits, command.startDate(), command.endDate(), folderName);
```

### 3. `AiSummaryAdapter.summarizeFolder()` 프롬프트 교체
`backend/.../report/adapter/out/ai/AiSummaryAdapter.java`

#### 3-1. PM 페르소나 프롬프트

```
당신은 전문적인 IT 프로젝트 매니저(PM)이자 주간보고서 작성 전문가입니다.
제공된 [커밋 목록]을 분석하여, 실무 엑셀 보고서의 작성 스타일과 완벽히 일치하는 보고서를 작성하세요.

[작성 가이드라인]
1. 문체: 반드시 "~함", "~완료", "~예정"으로 끝나는 격식 있는 개조식 문체 사용 (예: '수정 완료', '계획 수립', '검토 진행')
2. 구조화: 커밋을 '사업관리', '개발', 'QA/테스트' 카테고리로 논리적으로 분류
3. 상세도: 커밋 메시지를 나열하지 말고 비즈니스 관점에서 어떤 기능이 완성되었는지 요약
4. 차주 계획 추론:
   - 개발 커밋 비중이 높으면 → '통합 테스트 및 버그 수정 예정'
   - 기획/설계 커밋 비중이 높으면 → '개발 환경 구축 및 퍼블리싱 착수 예정'
   - 배포/운영 커밋이 있으면 → '운영 안정화 및 모니터링 예정'
   - 보안/인증 관련 커밋이 있으면 → '보안성 검토 및 취약점 점검 예정'
   금주 커밋 패턴을 분석하여 PM 관점에서 논리적인 다음 단계를 제안
5. 금지: 인사말, 설명, 머리말/꼬리말 절대 포함 금지 — [출력 형식]만 출력

[출력 형식]
[금주 진행사항 ({startMD}~{endMD})]
- {folderName}
    > 사업관리
    : 내용 (없으면 생략)
    > 개발
    : 내용 (없으면 생략)
    > QA/테스트
    : 내용 (없으면 생략)

[차주 진행계획 ({nextStartMD}~{nextEndMD})]
- {folderName}
    > 내용 (금주 작업과 이어지는 논리적 단계)

[커밋 목록]
{commitSection}
```

#### 3-2. stub fallback 수정
stub에서도 폴더명과 카테고리 구조 반영:
```java
private FolderAiSummaryResult stubSummarizeFolder(..., String folderName) {
    String progress = "[금주 진행사항]\n- " + folderName + "\n    > 개발\n    : " + stubSummarize(commits);
    String plan = "- " + folderName + "\n    > (stub) 차주 계획 자동 생성 불가 — 직접 입력해 주세요.";
    return new FolderAiSummaryResult(progress, plan, false);
}
```

#### 3-3. `parseFolderSummary()` — 기존 로직 유지
파싱 로직(`[금주 진행사항` / `[차주 진행계획` 구분자 기반)은 변경 없음.
새 프롬프트도 동일 구분자를 사용하므로 호환됨.

---

## 적용 순서

1. `AiSummaryPort` — `summarizeFolder()` 시그니처에 `folderName` 추가
2. `AiSummaryAdapter` — `summarizeFolder()` + `stubSummarizeFolder()` 프롬프트/파라미터 업데이트
3. `FolderSummaryAiSummarizeService` — `folderName` 전달

---

## 검증 방법

1. `ai.summary.enabled=false` → stub 출력에 폴더명·카테고리 구조 포함 확인
2. Gemini API 키 설정 후 폴더 AI 요약 호출 → 출력이 `사업관리`/`개발`/`QA` 카테고리 구조 포함 확인
3. 커밋 패턴별 차주 계획 추론 확인:
   - 개발 커밋 다수 → "통합 테스트" 언급
   - 배포 커밋 포함 → "운영 안정화" 언급
4. 기존 개인 보고서(`POST /api/reports/{id}/ai-summary`) 무변경 확인

---

## 영향받지 않는 파일
- DB 스키마 (컬럼 추가 없음)
- `FolderSummary` 도메인 모델
- `FolderSummaryResult` / `FolderSummaryResponse` DTO
- 프론트엔드 전체
- 개인 보고서 관련 파일 전체
