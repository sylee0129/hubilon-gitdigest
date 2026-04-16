# 주간보고 자동 업로드 스케줄러 구현 계획

## 요구사항 요약
- 매주 목요일 19:00 (KST) 자동 실행
- 대상: `FolderStatus.IN_PROGRESS` 상태인 모든 폴더
- 분기: `progressSummary` / `planSummary` 입력 여부에 따라 수동/AI 경로 분기
- 에러 발생 시 해당 폴더 스킵 → 다음 폴더 계속 진행
- 에러 내역 대시보드에서 조회 가능

---

## 분석 — 기존 구현 재사용 지점

| 재사용할 클래스 | 역할 |
|---|---|
| `FolderQueryUseCase` | 진행중 폴더 목록 조회 |
| `FolderSummaryQueryUseCase` | 해당 주의 FolderSummary 조회 |
| `FolderSummaryCreateUseCase` | FolderSummary 신규 생성 |
| `FolderSummaryAiSummarizeUseCase` | AI 요약 실행 |
| `UploadWeeklyReportUseCase` | Confluence 업로드 |

---

## 구현 계획

### [BE-1] 스케줄러 로그 엔티티 추가

**목적:** 스케줄러 실행 이력 및 에러 내역 저장

**새 파일:**
```
backend/src/main/java/com/hubilon/modules/scheduler/
├── domain/model/
│   ├── SchedulerJobLog.java          - 실행 로그 도메인 모델
│   └── SchedulerJobStatus.java       - RUNNING, SUCCESS, PARTIAL_FAIL, FAIL
├── domain/port/in/
│   └── SchedulerJobLogQueryUseCase.java
├── domain/port/out/
│   └── SchedulerJobLogCommandPort.java
│   └── SchedulerJobLogQueryPort.java
├── application/service/
│   └── SchedulerJobLogQueryService.java
├── adapter/
│   ├── in/web/SchedulerJobLogController.java   - GET /api/scheduler/logs
│   └── out/persistence/
│       ├── SchedulerJobLogJpaEntity.java
│       └── SchedulerJobLogPersistenceAdapter.java
```

**SchedulerJobLog 필드:**
```java
Long id
LocalDateTime executedAt        // 실행 시각
SchedulerJobStatus status       // RUNNING | SUCCESS | PARTIAL_FAIL | FAIL
int totalFolderCount            // 대상 폴더 수
int successCount                // 성공 수
int failCount                   // 실패 수
List<SchedulerFolderResult> folderResults  // 폴더별 결과
```

**SchedulerFolderResult 필드:**
```java
Long folderId
String folderName
boolean success
String errorMessage             // 실패 시 에러 메시지 (null if success)
String confluencePageUrl        // 성공 시 Confluence URL (null if fail)
```

**DB 테이블:**
```sql
CREATE TABLE scheduler_job_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    executed_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,  -- RUNNING, SUCCESS, PARTIAL_FAIL, FAIL
    total_folder_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    fail_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scheduler_folder_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_log_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    folder_name VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    error_message LONGTEXT,
    confluence_page_url VARCHAR(500),
    FOREIGN KEY (job_log_id) REFERENCES scheduler_job_logs(id) ON DELETE CASCADE
);
```

---

### [BE-2] 스케줄러 서비스 구현

**새 파일:**
```
backend/src/main/java/com/hubilon/modules/scheduler/
└── application/service/WeeklyReportSchedulerService.java
```

**실행 흐름:**
```
@Scheduled(cron = "0 0 19 * * THU", zone = "Asia/Seoul")
void executeWeeklyReport() {
    // 1. 실행 로그 생성 (RUNNING)
    // 2. 이번 주 기간 계산 (월~일)
    // 3. IN_PROGRESS 폴더 목록 조회
    // 4. 폴더별 루프 (try-catch로 에러 격리)
    //    a. 해당 기간 FolderSummary 조회
    //    b. 분기
    //       - progressSummary/planSummary 있음 → 바로 Confluence 업로드
    //       - 없음 → AI 요약 실행 → Confluence 업로드
    //    c. 성공/실패 결과 기록
    // 5. 로그 최종 상태 업데이트 (SUCCESS/PARTIAL_FAIL/FAIL)
}
```

**기간 계산 로직:**
```
실행일 = 목요일
startDate = 실행일 - 3일 (월요일)
endDate = 실행일 + 3일 (일요일)
```

**분기 조건:**
```java
if (folderSummary != null 
    && hasText(folderSummary.getProgressSummary()) 
    && hasText(folderSummary.getPlanSummary())) {
    // Manual 경로: 그대로 Confluence 업로드
} else {
    // Auto 경로
    if (folderSummary == null) {
        // FolderSummary 없음 → 먼저 분석 + AI 요약 실행
        folderSummaryCreateUseCase.create(...)
        folderSummaryAiSummarizeUseCase.summarize(...)
    } else {
        // FolderSummary 있으나 내용 없음 → AI 요약만 실행
        folderSummaryAiSummarizeUseCase.summarize(...)
    }
    // Confluence 업로드
}
uploadWeeklyReportUseCase.upload(...)
```

**Confluence 업로드 데이터 조립:**
- 모든 폴더의 결과를 `WeeklyReportRowDto` 리스트로 조립
- 멤버 정보: `FolderSummary`에서 커밋 기여자 추출 또는 프로젝트 담당자 사용

---

### [BE-3] 스케줄러 로그 조회 API

**엔드포인트:**
```
GET /api/scheduler/logs?page=0&size=10
  → 최근 실행 로그 목록 (페이징)

GET /api/scheduler/logs/{id}
  → 특정 실행 로그 상세 (폴더별 결과 포함)

POST /api/scheduler/trigger (수동 실행)
  → 즉시 실행 (테스트/재시도용)
```

**응답 예시:**
```json
{
  "id": 1,
  "executedAt": "2026-04-16T19:00:00",
  "status": "PARTIAL_FAIL",
  "totalFolderCount": 5,
  "successCount": 4,
  "failCount": 1,
  "folderResults": [
    {
      "folderId": 1,
      "folderName": "KT 사장이지 쇼핑센터 구축",
      "success": true,
      "confluencePageUrl": "https://..."
    },
    {
      "folderId": 2,
      "folderName": "신규 프로젝트 A",
      "success": false,
      "errorMessage": "GitLab API 연결 실패: Connection timeout"
    }
  ]
}
```

---

### [FE-1] 스케줄러 로그 대시보드 UI

**위치:** 기존 사이드바에 "스케줄러" 메뉴 추가 또는 설정 페이지 탭

**구성 요소:**
1. 최근 실행 로그 목록 (테이블)
   - 실행 시각 | 상태 뱃지 | 성공/실패 수 | 상세보기 버튼
2. 상세 모달/패널
   - 폴더별 결과 (성공 → 링크, 실패 → 에러 메시지)
3. 수동 실행 버튼 (POST /api/scheduler/trigger)
4. 상태 뱃지 색상
   - SUCCESS: green
   - PARTIAL_FAIL: yellow/orange
   - FAIL: red
   - RUNNING: blue (spinner)

---

## 구현 순서

```
[1] DB 마이그레이션 스크립트 작성
    └─ scheduler_job_logs, scheduler_folder_results 테이블 추가

[2] 도메인 모델 + Port 인터페이스 작성
    └─ SchedulerJobLog, SchedulerJobStatus, SchedulerFolderResult

[3] JPA 엔티티 + 퍼시스턴스 어댑터 구현

[4] WeeklyReportSchedulerService 구현 (핵심 로직)

[5] SchedulerJobLogController 구현 (조회 + 수동 실행 API)

[6] Spring @EnableScheduling 설정 확인/추가

[7] FE: 스케줄러 로그 조회 UI 구현
```

---

## 의존성 주입 구조

```
WeeklyReportSchedulerService
  ├── FolderQueryUseCase            (기존)
  ├── FolderSummaryQueryUseCase     (기존)
  ├── FolderSummaryCreateUseCase    (기존)
  ├── FolderSummaryAiSummarizeUseCase (기존)
  ├── UploadWeeklyReportUseCase     (기존)
  └── SchedulerJobLogCommandPort    (신규)
```

---

## 고려사항

- `@EnableScheduling` → `Application` 클래스 또는 별도 `SchedulerConfig`에 추가
- `spring.task.scheduling.pool.size=1` 설정 (단일 스레드, 중복 실행 방지)
- 이미 `RUNNING` 상태인 로그가 있으면 재실행 방지 (동시 실행 방지)
- AI 요약 실패(`aiSummaryFailed=true`) 시에도 Confluence 업로드는 시도 (내용 없이 빈 값으로 업로드)
- `FolderSummaryCreateUseCase`가 이미 해당 기간 레코드를 생성한 경우 중복 생성 방지 → UNIQUE KEY 활용

---

## Review 결과 및 이슈 반영

- 검토일: 2026-04-16
- 검토 항목: 보안 / 리팩토링 / 기능

### 이슈 반영 내용

#### 보안
- **보안-1** `POST /api/scheduler/trigger` → `@PreAuthorize("hasRole('ADMIN')")` 적용
- **보안-2** `errorMessage` 컬럼에는 exception.getMessage()만 저장, 스택트레이스는 서버 로그(log.error)로만 기록
- **보안-3** `GET /api/scheduler/logs`, `GET /api/scheduler/logs/{id}` → 인증된 사용자 전용 (Spring Security 설정에 `/api/scheduler/**` 인증 필수 추가)

#### 리팩토링
- **리팩토링-1** ShedLock 도입 (`net.javacrumbs.shedlock:shedlock-spring`) — DB 기반 분산 락으로 멀티 인스턴스 중복 실행 방지. `@SchedulerLock(name = "weeklyReport", lockAtLeastFor = "PT1H", lockAtMostFor = "PT2H")` 적용
- **리팩토링-2** `WeeklyReportUploadStrategy` 인터페이스 + `ManualUploadStrategy`, `AiUploadStrategy` 구현체로 분기 로직 분리
- **리팩토링-3** `WeeklyReportProcessor` 중간 서비스 도입 — 폴더별 처리 로직 캡슐화, 스케줄러는 오케스트레이션(루프 + 로그 관리)만 담당

#### 기능
- **기능-1** 기간 계산은 **ISO 8601 주차 기준** — `LocalDate.now().with(DayOfWeek.MONDAY)` / `.with(DayOfWeek.SUNDAY)` 사용 (연도 경계 자동 처리)
- **기능-2** AI 요약 실패 시 폴백: 빈 progressSummary/planSummary로 Confluence 업로드 진행 (폴더 스킵하지 않음), `aiSummaryFailed=true` 기록
- **기능-3** `POST /api/scheduler/trigger` 호출 시 RUNNING 상태 존재하면 `409 Conflict` 반환 (`"스케줄러가 이미 실행 중입니다."`)

### 최종 결과: 이슈 반영 완료 → 구현 진행
