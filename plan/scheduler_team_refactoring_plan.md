# 스케줄러 팀 단위 고도화 — 구현 계획

## 현재 구조 요약

| 항목 | 현재 상태 |
|------|-----------|
| `trigger()` | 팀 구분 없이 전체 폴더 일괄 실행 |
| `SchedulerJobLog` | 팀 정보 없음 |
| 수동 실행 UI | 단일 버튼, 권한 구분 없음 |
| 실행 이력 테이블 | 팀 컬럼 없음 |
| 팀별 On/Off 설정 | 없음 |

---

## 변경 범위

### 백엔드

#### 1. `SchedulerTeamConfig` — 팀별 스케줄러 활성화 설정 (신규)

**도메인 모델** `scheduler/domain/model/SchedulerTeamConfig.java`
```java
public record SchedulerTeamConfig(Long id, Long teamId, String teamName, boolean enabled) {
    public static SchedulerTeamConfig create(Long teamId, String teamName) {
        return new SchedulerTeamConfig(null, teamId, teamName, true);
    }
    public SchedulerTeamConfig toggle(boolean enabled) {
        return new SchedulerTeamConfig(id, teamId, teamName, enabled);
    }
}
```

**JPA 엔티티** `scheduler/adapter/out/persistence/SchedulerTeamConfigJpaEntity.java`
- 테이블: `scheduler_team_configs`
- 컬럼: `id`, `team_id(UNIQUE)`, `team_name`, `enabled`, `created_at`, `updated_at`

**리포지토리** `SchedulerTeamConfigRepository`
- `findByTeamId(Long teamId)`
- `findAllByEnabled(boolean enabled)`

**UseCase 포트**
- `SchedulerTeamConfigUseCase` (in)
  - `findAll() -> List<SchedulerTeamConfig>`
  - `upsert(Long teamId, boolean enabled) -> SchedulerTeamConfig`

**PersistenceAdapter**
- `SchedulerTeamConfigPersistenceAdapter` implements 위 out port

**Service** `SchedulerTeamConfigService`
- ADMIN 권한 검증 후 팀 설정 조회/변경

---

#### 2. `SchedulerJobLog` — teamId, teamName 필드 추가

**도메인 모델 변경**
```java
// 기존
SchedulerJobLog createRunning()

// 변경
SchedulerJobLog createRunning(Long teamId, String teamName)
```
- 필드 추가: `Long teamId`, `String teamName`

**JPA 엔티티 변경**
- `scheduler_job_logs` 테이블에 `team_id`, `team_name` 컬럼 추가

**Response DTO 변경** `SchedulerJobLogResponse`
- `teamId`, `teamName` 필드 추가

---

#### 3. `trigger()` — teamId 파라미터 추가

**UseCase 포트 변경** `SchedulerTriggerUseCase`
```java
// 기존
SchedulerJobLog trigger();

// 변경
SchedulerJobLog trigger(Long teamId);
```

**`WeeklyReportSchedulerService` 변경**
- `executeWeeklyReport()` (cron): `enabled=true`인 팀 목록 조회 → 팀별 순차 실행
  - 팀별로 각각 `SchedulerJobLog` 생성 (팀 정보 포함)
  - 각 팀의 IN_PROGRESS 폴더만 필터링
- `trigger(Long teamId)` (수동): 특정 팀의 폴더만 실행

**Controller 변경** `POST /api/scheduler/trigger`
```java
// 기존
@PostMapping("/trigger")
public ResponseEntity<SchedulerJobLogResponse> trigger()

// 변경
@PostMapping("/trigger")
public ResponseEntity<SchedulerJobLogResponse> trigger(@RequestBody TriggerRequest request)

// TriggerRequest: record { Long teamId }
```

---

#### 4. 팀 설정 관리 API (신규, ADMIN 전용)

**Controller** `SchedulerTeamConfigController` — `/api/scheduler/team-configs`
- `GET /` — 전체 팀 설정 목록 조회 (ADMIN)
- `PUT /{teamId}` — 특정 팀 On/Off 토글 (ADMIN)
  - Request: `{ "enabled": true/false }`

**권한 체크**
- `@PreAuthorize("hasRole('ADMIN')")` 또는 현재 사용하는 방식에 맞게 적용

---

#### 5. 사용자 팀 조회

**`trigger(Long teamId)` 호출 시 일반 사용자 처리**
- Controller에서 현재 로그인 사용자의 `teamId` 조회
- `teamId` 파라미터가 null이면 → 본인 팀 ID로 자동 셋팅
- ADMIN이면 request body의 `teamId` 사용

---

### 프론트엔드

#### 1. 실행 이력 테이블 — 팀 컬럼 추가

**위치**: 스케줄러 로그 목록 테이블
- 기존 컬럼 사이에 `팀` 컬럼 추가
- API 응답의 `teamName` 값 표시
- 팀 정보 없을 경우 `-` 표시

---

#### 2. 수동 실행 버튼 옆 팀 선택 드롭다운 (ADMIN 전용)

**컴포넌트**: 스케줄러 페이지 내 수동 실행 영역

```
[팀 선택 ▼]  [수동 실행]    ← ADMIN
             [수동 실행]    ← 일반 사용자 (드롭다운 없음)
```

- ADMIN 여부는 기존 인증 상태 판단 로직 사용
- 드롭다운 목록: `GET /api/scheduler/team-configs` 로 팀 목록 조회
- 선택된 `teamId`를 trigger 요청에 포함: `POST /api/scheduler/trigger { teamId }`
- 일반 사용자는 `teamId` 없이 요청 → 백엔드에서 본인 팀 자동 셋팅

---

#### 3. 팀별 스케줄러 설정 페이지 (ADMIN 전용 신규 페이지)

**라우트**: `/scheduler/team-settings` 또는 기존 스케줄러 페이지 내 탭

**UI 구성**:
- 팀 목록 테이블
  - 컬럼: 팀명, 스케줄러 상태 (On/Off 토글 스위치)
- 토글 변경 시 즉시 `PUT /api/scheduler/team-configs/{teamId}` 호출
- ADMIN이 아닌 경우 해당 페이지/탭 미노출 또는 접근 차단

---

## DB 마이그레이션

### 신규 테이블: `scheduler_team_configs`
```sql
CREATE TABLE scheduler_team_configs (
    id          BIGSERIAL PRIMARY KEY,
    team_id     BIGINT NOT NULL UNIQUE,
    team_name   VARCHAR(100) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### 기존 테이블 변경: `scheduler_job_logs`
```sql
ALTER TABLE scheduler_job_logs
    ADD COLUMN team_id   BIGINT,
    ADD COLUMN team_name VARCHAR(100);
```
- 기존 데이터는 null 허용 (레거시 실행 이력)

---

## 구현 순서

### Phase 1 — 백엔드 (의존 없음)
1. `SchedulerTeamConfig` 도메인 모델 + JPA 엔티티 + 리포지토리 생성
2. `scheduler_team_configs` 테이블 생성 (직접 DDL 또는 @Table로 자동 생성)
3. `SchedulerTeamConfigService` + Controller 구현
4. `SchedulerJobLog` 모델/엔티티에 `teamId`, `teamName` 추가
5. `scheduler_job_logs` 테이블에 컬럼 추가
6. `trigger(Long teamId)` 시그니처 변경 + 서비스 로직 수정
7. `executeWeeklyReport()` — enabled 팀만 필터링 로직 추가
8. Controller `POST /trigger` 요청 DTO 변경 + 권한별 teamId 처리

### Phase 2 — 프론트엔드 (백엔드 완료 후)
1. 실행 이력 테이블에 팀 컬럼 추가
2. 수동 실행 영역에 ADMIN 전용 팀 드롭다운 추가
3. 팀별 스케줄러 설정 페이지(또는 탭) 구현

---

## 영향 범위 체크리스트

- [ ] `SchedulerTriggerUseCase` 인터페이스 변경 → 기존 `trigger()` 호출부 전부 수정
- [ ] `WeeklyReportSchedulerService.executeWeeklyReport()` — 팀별 루프로 전환
- [ ] `SchedulerJobLog.createRunning()` 시그니처 변경 → 호출부 수정
- [ ] Response DTO 변경 → 프론트엔드 타입 업데이트
- [ ] 기존 실행 이력 데이터 — `team_id`, `team_name` null 처리 (UI에서 `-` 표시)

---

## 설계 결정 사항 (리뷰 반영)

### 보안
- `trigger()` Controller: `SecurityContext`에서 역할 확인 → ADMIN이면 request body teamId 사용, 일반 사용자면 본인 teamId로 강제 덮어쓰기
- ADMIN 전용 API(`GET/PUT /team-configs`): `@PreAuthorize("hasRole('ADMIN')")` 적용

### 수동 실행 enabled 체크
- **ADMIN**: enabled=false 팀도 강제 실행 가능 (enabled 체크 생략)
- **일반 사용자**: 본인 팀 자동 셋팅 (enabled 체크 없음, 단순 본인 팀 실행)
- **cron 자동 실행만**: enabled=true 팀만 필터링

### 드롭다운 데이터 출처
- `Team` 도메인 기준 전체 팀 조회 (enabled 여부 무관)
- `scheduler_team_configs`에 없는 신규 팀 → enabled=true 기본값으로 간주
- `GET /api/scheduler/team-configs` 응답: 전체 팀 목록 (enabled 상태 포함)

### 팀 삭제 시 정합성
- `scheduler_team_configs.team_id`에 FK + `ON DELETE CASCADE` 적용
- `scheduler_job_logs.team_id`는 히스토리 보존 목적으로 FK 미적용 (null 유지)

### upsert teamName 출처
- `PUT /team-configs/{teamId}` 호출 시 `Team` 리포지토리에서 teamName 조회 후 저장

---

## Review 결과
- 검토일: 2026-04-22
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 → 설계 결정 사항에 반영 완료
