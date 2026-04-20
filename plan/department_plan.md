# 조직 계층 구조(실-팀) 도입 계획

## 목표
`departments` 테이블 추가 + `teams` 테이블에 FK 연결로 실-팀 계층 구조 구현

---

## 계층 데이터 예시

| 실(Department) | 팀(Team) |
| :--- | :--- |
| 플랫폼개발실 | 플랫폼개발팀 |
| 서비스개발실 | 서비스개발1팀, 서비스개발2팀, 서비스개발3팀 |
| 솔루션연구소 | 솔루션개발1팀, 솔루션개발2팀 |

---

## Phase 1 — DB 스키마

### 1-1. departments 테이블 생성
```sql
CREATE TABLE departments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
```

### 1-2. teams 테이블 수정
```sql
ALTER TABLE teams ADD COLUMN dept_id BIGINT;
ALTER TABLE teams ADD CONSTRAINT fk_teams_dept FOREIGN KEY (dept_id) REFERENCES departments(id);
```

### 1-3. 초기 데이터 INSERT
```sql
INSERT INTO departments (name, created_at, updated_at) VALUES
('플랫폼개발실', NOW(), NOW()),
('서비스개발실', NOW(), NOW()),
('솔루션연구소', NOW(), NOW());

-- teams 데이터에 dept_id 매핑 (실제 팀 ID 확인 후 적용)
UPDATE teams SET dept_id = 1 WHERE name = '플랫폼개발팀';
UPDATE teams SET dept_id = 2 WHERE name IN ('서비스개발1팀','서비스개발2팀','서비스개발3팀');
UPDATE teams SET dept_id = 3 WHERE name IN ('솔루션개발1팀','솔루션개발2팀');
```

---

## Phase 2 — 백엔드

### 2-1. Department 도메인 모듈 생성
패키지: `com.hubilon.modules.department`
- **도메인 모델**: `Department` (id, name, createdAt, updatedAt)
- **JPA 엔티티**: `DepartmentJpaEntity`
- **Repository**: `DepartmentJpaRepository`
- **포트**: `DepartmentQueryPort`
- **유스케이스**: `DepartmentQueryUseCase`
- **서비스**: `DepartmentQueryService`
- **컨트롤러**: `DepartmentController`
  - `GET /api/departments` — 전체 실 목록 조회
  - `GET /api/departments/{deptId}/teams` — 특정 실 소속 팀 목록 조회

### 2-2. Team 도메인 수정
- `TeamJpaEntity`에 `deptId` 필드 추가
- `Team` 도메인 모델에 `deptId` 추가
- 팀 조회 시 `deptId` 포함하여 반환
- `TeamJpaRepository`에 `findByDeptId(Long deptId)` 추가

### 2-3. 응답 DTO
```java
// DepartmentResponse
{
  "id": 1,
  "name": "플랫폼개발실"
}

// DepartmentWithTeamsResponse
{
  "id": 1,
  "name": "플랫폼개발실",
  "teams": [
    { "id": 10, "name": "플랫폼개발팀" }
  ]
}
```

---

## Phase 3 — 프론트엔드

### 3-1. API 연동
- `GET /api/departments` 호출하는 `useDepartments` 훅 추가
- `TeamResponse` 타입에 `deptId` 필드 추가

### 3-2. UI 수정 (사이드바 / 팀 선택 영역)
- 팀 목록을 실 단위로 그룹핑하여 표시
- 실 헤더 → 하위 팀 목록 구조로 렌더링

---

## 작업 순서

1. [x] DB 마이그레이션 (Flyway V4)
2. [x] 백엔드 — Department 모듈 구현
3. [x] 백엔드 — Team 모듈 deptId 연동
4. [-] 프론트엔드 — UI 그룹핑 (사용자 요청으로 제외)

---

## 파일 영향 범위

### 신규
- `backend/.../department/` — 전체 모듈
- `frontend/src/hooks/useDepartments.ts`

### 수정
- `backend/.../team/domain/model/Team.java`
- `backend/.../team/adapter/out/persistence/TeamJpaEntity.java`
- `backend/.../team/adapter/out/persistence/TeamJpaRepository.java`
- `backend/.../team/adapter/out/persistence/TeamPersistenceAdapter.java`
- `frontend/src/hooks/useTeams.ts` (또는 유사 파일)
- `frontend/src/components/layout/Sidebar` 관련 컴포넌트
- DB 마이그레이션 파일 (`V*.sql` 또는 `schema.sql`)

---

## Review 결과
- 검토일: 2026-04-20
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 10건 식별 → 사용자 확인 후 반영 (7번 팀 없는 실 없음, 9번 UI 제외)
- 테스트: `./gradlew test` BUILD SUCCESSFUL (70 tests passed)
