# 프로젝트 관리 및 폴더 구조화 기능 계획

**작성일:** 2026-04-09  
**최종수정:** 2026-04-09  
**대상 모듈:** Backend (`modules/folder`, `modules/workproject`, `modules/user`) + Frontend

---

## Overview

사용자가 '프로젝트 폴더'를 생성하고, 폴더 하위에 세부 프로젝트를 관리하는 계층 구조를 제공한다.  
기존 `Project` 엔티티(GitLab 연동용)와 분리하여, 업무 단위의 폴더/프로젝트 관리 도메인을 신규 추가한다.

폴더는 단순한 컨테이너가 아닌 **프로젝트 단위** 그 자체이며, 구분/프로젝트명/담당자/상태를 보유한다.

```
[폴더] (구분, 프로젝트명, 담당자, 상태)
  └─ [세부 프로젝트] (명칭)
  └─ [세부 프로젝트] (명칭)
  └─ ...
```

---

## Features

### 폴더 관리
- [ ] 폴더 생성 (구분, 프로젝트명, 담당자 다중 선택, 상태 입력)
- [ ] 폴더 수정 (구분, 프로젝트명, 담당자, 상태 변경)
- [ ] 폴더 삭제 (하위 세부 프로젝트가 있을 경우 경고)
- [ ] 폴더 정렬 순서 변경 (드래그앤드롭)
- [ ] 폴더 접기/펼치기 (트리뷰)
- [ ] 상태별 필터링 표시 (진행중 / 완료)

### 세부 프로젝트 관리 (폴더 하위)
- [ ] 세부 프로젝트 등록 (명칭 입력, 소속 폴더)
- [ ] 세부 프로젝트 수정
- [ ] 세부 프로젝트 삭제
- [ ] 세부 프로젝트 정렬 순서 변경 (폴더 내 드래그앤드롭)

### 담당자 관리
- [ ] 시스템 사용자 목록 조회 API (이름/이메일 검색) — `?q=` 파라미터 미구현
- [ ] 폴더에 담당자 추가/제거 — Folder 모듈 미구현
- [ ] 칩(Chip) 형태 UI로 선택된 담당자 표시 — 프론트엔드 미구현

### 사용자 기본 관리 (담당자 풀)
- [x] 사용자 목록 조회 — `GET /api/users` 구현 완료
- [ ] 사용자 등록 (이름, 이메일, 부서) — 서비스 로직은 구현, Controller 엔드포인트 미구현
- [ ] 사용자 삭제 — UseCase 및 Controller 미구현

---

## Database Schema

### 기존 테이블 (변경 없음)
```
projects (id, name, gitlab_url, access_token, auth_type, gitlab_project_id, sort_order, created_at, updated_at)
```

### 신규 테이블

#### `folders` — 프로젝트 폴더 (구분/프로젝트명/상태 포함)
```sql
CREATE TABLE folders (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(200) NOT NULL,               -- 프로젝트명
  category    VARCHAR(20)  NOT NULL,               -- ENUM: DEVELOPMENT | NEW_BUSINESS | OTHER
  status      VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',  -- ENUM: IN_PROGRESS | COMPLETED
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### `folder_members` — 폴더-담당자 매핑 (N:M)
```sql
CREATE TABLE folder_members (
  folder_id  BIGINT NOT NULL,
  user_id    BIGINT NOT NULL,
  PRIMARY KEY (folder_id, user_id),
  FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
);
```

#### `work_projects` — 세부 프로젝트 (폴더 하위, 명칭만 보유)
```sql
CREATE TABLE work_projects (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  folder_id   BIGINT NOT NULL,
  name        VARCHAR(200) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
);
```

#### `users` — 시스템 사용자 (담당자 풀)
```sql
CREATE TABLE users (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  email       VARCHAR(100) NOT NULL UNIQUE,
  department  VARCHAR(100),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Enum 값 정의

**category**
| 값 | 표시명 |
|----|--------|
| `DEVELOPMENT` | 개발사업 |
| `NEW_BUSINESS` | 신규추진사업 |
| `OTHER` | 기타 |

**status**
| 값 | 표시명 |
|----|--------|
| `IN_PROGRESS` | 진행중 |
| `COMPLETED` | 완료 |

### ERD 요약
```
folders (1) ──── (N) work_projects
    │
    └── (N:M) users  [via folder_members]
```

---

## API Design

### Folder API — `/api/folders`

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/folders` | 전체 폴더 목록 (`?status=IN_PROGRESS\|COMPLETED` 필터 지원) |
| POST | `/api/folders` | 폴더 생성 |
| PUT | `/api/folders/{id}` | 폴더 수정 |
| DELETE | `/api/folders/{id}` | 폴더 삭제 (`?force=true` 시 하위 포함 강제 삭제) |
| PATCH | `/api/folders/reorder` | 폴더 순서 변경 |

**POST /api/folders 요청**
```json
{
  "name": "차세대 ERP 구축",
  "category": "DEVELOPMENT",
  "status": "IN_PROGRESS",
  "memberIds": [1, 2, 3]
}
```
> `category` 허용값: `DEVELOPMENT` | `NEW_BUSINESS` | `OTHER`  
> `status` 허용값: `IN_PROGRESS` | `COMPLETED`  
> 서버에서 ENUM 유효성 검증 필수 (허용값 외 → 400 Bad Request)

**DELETE /api/folders/{id} 동작 규칙**
- 하위 `work_projects`가 없을 경우 → 즉시 삭제 (204)
- 하위 `work_projects`가 있을 경우
  - `?force=true` 없으면 → 409 Conflict + 하위 프로젝트 개수 반환
  - `?force=true` 있으면 → 하위 포함 일괄 삭제 (204)

```json
// 409 응답 예시
{
  "message": "하위 세부 프로젝트가 2개 존재합니다.",
  "workProjectCount": 2
}
```

**PATCH /api/folders/reorder 요청**
```json
{
  "orders": [
    { "id": 1, "sortOrder": 0 },
    { "id": 3, "sortOrder": 1 },
    { "id": 2, "sortOrder": 2 }
  ]
}
```

**GET /api/folders 응답**
> N+1 방지: `folders` + `folder_members` + `work_projects` 를 Fetch Join 단일 쿼리로 조회
```json
[
  {
    "id": 1,
    "name": "차세대 ERP 구축",
    "category": "DEVELOPMENT",
    "status": "IN_PROGRESS",
    "sortOrder": 0,
    "members": [
      { "id": 1, "name": "홍길동", "department": "개발팀" }
    ],
    "workProjects": [
      { "id": 10, "name": "요구사항 분석", "sortOrder": 0 },
      { "id": 11, "name": "설계 및 개발", "sortOrder": 1 }
    ]
  }
]
```
> `members` 응답에서 `email` 제외 (UI 표시 불필요, 노출 최소화)

---

### WorkProject API — `/api/work-projects`

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/work-projects` | 세부 프로젝트 등록 |
| PUT | `/api/work-projects/{id}` | 세부 프로젝트 수정 (폴더 이동 포함) |
| DELETE | `/api/work-projects/{id}` | 세부 프로젝트 삭제 |
| PATCH | `/api/work-projects/reorder` | 세부 프로젝트 순서 변경 |

**POST /api/work-projects 요청**
```json
{
  "folderId": 1,
  "name": "요구사항 분석"
}
```

**PUT /api/work-projects/{id} 요청** (폴더 이동 가능)
```json
{
  "folderId": 2,
  "name": "요구사항 분석"
}
```

**PATCH /api/work-projects/reorder 요청**
```json
{
  "folderId": 1,
  "orders": [
    { "id": 10, "sortOrder": 0 },
    { "id": 11, "sortOrder": 1 }
  ]
}
```

---

### User API — `/api/users`

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/users` | 사용자 목록 (`?q=검색어` 이름/이메일 필터) |
| POST | `/api/users` | 사용자 등록 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |

**POST /api/users 요청**
```json
{
  "name": "홍길동",
  "email": "hong@example.com",
  "department": "개발팀"
}
```

**DELETE /api/users/{id} 동작 규칙**
- 삭제 전 `folder_members`에서 해당 user_id 참조 폴더 수 확인
- 담당자로 지정된 폴더가 있을 경우 → 409 Conflict + 참조 폴더 목록 반환
- 폴더 참조 없을 경우 → 즉시 삭제 (204)

```json
// 409 응답 예시
{
  "message": "해당 사용자가 담당자로 지정된 프로젝트가 1개 존재합니다.",
  "referencedFolders": [{ "id": 1, "name": "차세대 ERP 구축" }]
}
```

---

## UI/UX Plan

### 사이드바 트리뷰 (Sidebar.tsx 확장)

```
[+ 폴더 추가]

▼ 차세대 ERP 구축  [진행중]  [⋮]
    ├─ 요구사항 분석
    └─ 설계 및 개발

▼ 모바일 앱 리뉴얼  [완료]   [⋮]
    └─ UI 개선

▶ 내부 인프라 개선  [진행중]  [⋮]
```

- 폴더 클릭 → 접기/펼치기
- 상태 배지(`[진행중]` / `[완료]`) 색상 구분 — 진행중: 파랑, 완료: 회색
- 폴더 우측 `[⋮]` → 수정/삭제 컨텍스트 메뉴
- 세부 프로젝트 클릭 → 보고서 패널 연동
- 드래그앤드롭으로 폴더 간, 폴더 내 순서 변경

### 폴더 생성/수정 모달

```
┌───────────────────────────────────────┐
│  프로젝트 폴더 추가                    │
├───────────────────────────────────────┤
│  구분        [개발사업            ▼]  │
│  프로젝트명  [________________________]│
│  담당자      [홍길동 ×] [이순신 ×]    │
│              [검색어 입력...     🔍]  │
│              ┌───────────────────┐    │
│              │ ● 홍길동 (개발팀) │    │
│              │ ● 이순신 (기획팀) │    │
│              └───────────────────┘    │
│  상태        ○ 진행중   ○ 완료        │
├───────────────────────────────────────┤
│                    [취소]  [저장]      │
└───────────────────────────────────────┘
```

**담당자 선택 UX:**
1. 검색창에 이름/이메일 입력 → 실시간 필터링 드롭다운
2. 선택 시 칩(Chip) 형태로 표시 (`이름 ×`)
3. 칩의 `×` 클릭 → 담당자 제거

**상태 UX:**
- Radio 버튼 또는 토글 형태
- 기본값: 진행중

---

## Implementation Order

### Phase 1 — Backend
1. `modules/user/` 도메인 모듈 보완
   - [x] User 엔티티, 도메인 모델, `GET /api/users` 구현 완료
   - [ ] `POST /api/users` 엔드포인트 추가 (서비스 로직은 구현됨)
   - [ ] `DELETE /api/users/{id}` UseCase + Controller 추가 (folder_members 참조 시 409)
   - [ ] `GET /api/users?q=` 이름/이메일 검색 파라미터 추가
2. `modules/folder/` 도메인 모듈 생성
   - [ ] Folder 엔티티 (name, category, status, sortOrder)
   - [ ] `folder_members` 매핑 엔티티
   - [ ] CRUD UseCase + `/api/folders` 컨트롤러
   - [ ] Fetch Join 단일 쿼리 (N+1 방지)
3. `modules/workproject/` 도메인 모듈 생성
   - [ ] WorkProject 엔티티 (name, folderId, sortOrder)
   - [ ] CRUD UseCase + `/api/work-projects` 컨트롤러

### Phase 2 — Frontend
1. [ ] `types/folder.ts` — Folder, WorkProject, User 타입 정의
2. [ ] `services/folderApi.ts`, `services/workProjectApi.ts`, `services/userApi.ts`
3. [ ] `hooks/useFolders.ts`, `hooks/useWorkProjects.ts`, `hooks/useUsers.ts`
4. [ ] `components/folder/FolderModal.tsx` — 구분/프로젝트명/담당자/상태 입력, 담당자 Multi-select Chip UI
5. [ ] `Sidebar.tsx` — 폴더 트리뷰 렌더링, 상태 배지, 드래그앤드롭 확장

### Phase 3 — 연동 테스트
1. 폴더 생성 → 담당자 선택 → 세부 프로젝트 등록 전체 플로우 확인
2. 상태 변경 (진행중 ↔ 완료) 반영 확인
3. 사이드바에서 세부 프로젝트 클릭 시 보고서 패널 연동 확인

---

## 미결 사항

- 기존 GitLab `Project`와 `WorkProject`의 연동 여부 — 세부 프로젝트에 GitLab 프로젝트를 연결할 수 있도록 추후 확장 가능
- 사용자 인증 시스템 부재 → 담당자는 단순 참조 데이터로만 관리 (로그인 기능 없음), API 인증은 이번 범위 제외
- 완료 상태 폴더를 별도 섹션으로 분리할지 (진행중 목록 / 완료 목록 탭 구분) 추후 결정
- 폴더/세부 프로젝트 수 증가 시 페이지네이션 도입 검토 (현재는 전체 반환)

---

## Review 결과

- 검토일: 2026-04-09
- 검토 항목: 보안 / 리팩토링 / 기능
- 반영 항목:
  - [H2] 폴더 삭제 동작 정의: 하위 있을 시 409, `?force=true` 강제 삭제 추가
  - [H3] 세부 프로젝트 폴더 이동: `PUT /api/work-projects/{id}` 요청에 `folderId` 포함
  - [M1] reorder 요청 바디 스펙 명시 (폴더/세부 프로젝트 공통 `{ orders: [{id, sortOrder}] }`)
  - [M2] N+1 방지: `GET /api/folders` Fetch Join 전략 명시
  - [M3] 사용자 삭제 시 담당자 참조 폴더 있을 경우 409 반환
  - [L1] ENUM 유효성 검증 필수 명시 (400 Bad Request)
  - [L2] 상태 필터 쿼리 파라미터 추가: `GET /api/folders?status=`
  - [L3] 이메일 노출 최소화: `members` 응답에서 email 제외
- 미반영 항목: [H1] 인증/인가 — 기존 시스템과 동일 미결 상태, 이번 범위 제외
