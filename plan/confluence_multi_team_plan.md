# Confluence 연동 정보 동적 관리 및 멀티 테넌트 적용 — Plan

## 목표
- `application.yml`의 고정 Confluence 설정을 DB로 이전
- 실(Department) 단위 인증 정보, 팀 단위 업로드 경로 동적 관리
- 관리 페이지(React) 제공

---

## 1. DB 스키마

### confluence_space_configs (실 단위)
```sql
CREATE TABLE confluence_space_configs (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id     BIGINT NOT NULL REFERENCES departments(id),
    user_email  VARCHAR(255) NOT NULL,
    api_token   VARCHAR(1000) NOT NULL,   -- AES-256-GCM 암호화 저장 (IV 포함)
    space_key   VARCHAR(100) NOT NULL,
    base_url    VARCHAR(500) NOT NULL,    -- 화이트리스트 검증 적용
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (dept_id)
);
```

### confluence_team_configs (팀 단위)
```sql
CREATE TABLE confluence_team_configs (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id        BIGINT NOT NULL REFERENCES teams(id),
    parent_page_id VARCHAR(100) NOT NULL,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (team_id)
);
```

> **전제 검증**: `teams` 테이블에 `dept_id` 컬럼이 존재해야 함. 마이그레이션 시작 전 확인 필수.

---

## 2. 백엔드 구조

### 2-1. 패키지 레이아웃
```
backend/src/main/java/.../
├── confluence/
│   ├── config/
│   │   ├── ConfluenceSpaceConfig.java        (Entity)
│   │   ├── ConfluenceTeamConfig.java         (Entity)
│   │   ├── ConfluenceSpaceConfigRepository.java
│   │   ├── ConfluenceTeamConfigRepository.java
│   │   ├── ConfluenceConfigService.java      (Space + Team 통합 서비스)
│   │   └── ConfluenceConfigController.java   (관리 API)
│   ├── client/
│   │   ├── ConfluenceClient.java             (동적 인증 적용)
│   │   └── ConfluenceClientCache.java        (dept별 캐시, 설정 변경 시 무효화)
│   └── upload/
│       └── ConfluenceUploadService.java      (팀→실 매핑 로직 + 실패 처리)
└── crypto/
    └── AesEncryptionService.java             (AES-256-GCM, IV 포함 저장)
```

### 2-2. AES-256-GCM 암호화 전략
- IV(12 bytes)를 매 암호화마다 `SecureRandom`으로 신규 생성
- DB 저장 형식: `Base64(IV) + ":" + Base64(CipherText+AuthTag)`
- 복호화 시 저장된 IV를 파싱하여 사용 → IV 재사용 완전 차단
- 암호화 키: 환경변수 `CONFLUENCE_TOKEN_SECRET` (32바이트, Base64 인코딩)
- **키 로테이션**: `CONFLUENCE_TOKEN_SECRET_NEW` 환경변수 추가 → 로그인 시 구버전 토큰 자동 재암호화 후 신규 키로 교체. 레코드 단위 마이그레이션 배치 제공.

### 2-3. 핵심 로직 — 팀→실 매핑
```
업로드 요청(teamId)
  → teams.dept_id 조회 (dept_id 없으면 → 409 ConfigNotFoundException 반환)
  → ConfluenceClientCache.get(deptId)
      → 캐시 히트: 캐시된 ConfluenceClient 반환
      → 캐시 미스: confluence_space_configs WHERE dept_id = deptId 조회
                   (레코드 없으면 → 404 SpaceConfigNotFoundException 반환)
                   api_token 복호화 (로그 출력 절대 금지)
                   ConfluenceClient 생성 후 캐시 저장
  → confluence_team_configs WHERE team_id = teamId → parent_page_id
      (레코드 없으면 → 404 TeamConfigNotFoundException 반환)
  → Confluence API 호출
```

### 2-4. ConfluenceClientCache 전략
- dept별 `ConcurrentHashMap` 캐시
- 설정 저장(upsert) / 삭제 시 해당 dept 캐시 즉시 무효화
- 캐시 TTL: 1시간 (Caffeine 또는 직접 구현)

### 2-5. Base URL SSRF 방어
- 허용 도메인 화이트리스트: `CONFLUENCE_ALLOWED_HOSTS` 환경변수 (콤마 구분)
- 저장 전 서버 측 검증. 불일치 시 400 반환.

### 2-6. 제거 대상
- `application.yml`의 `confluence.*` 키 전체
- `@Value("${confluence.*}")` 사용 코드 전체

### 2-7. API 엔드포인트
| Method | Path | 설명 |
|--------|------|------|
| GET    | /api/admin/confluence/spaces | 실별 설정 목록 (api_token 마스킹) |
| POST   | /api/admin/confluence/spaces | 실별 설정 upsert |
| DELETE | /api/admin/confluence/spaces/{deptId} | 실별 설정 삭제 + 캐시 무효화 |
| GET    | /api/admin/confluence/teams  | 팀별 경로 목록 |
| POST   | /api/admin/confluence/teams  | 팀별 경로 upsert |
| DELETE | /api/admin/confluence/teams/{teamId} | 팀별 경로 삭제 |
| POST   | /api/admin/confluence/spaces/{deptId}/test | Confluence 연결 테스트 |

### 2-8. 응답 DTO
```java
// SpaceConfigResponse — api_token 필드 제외, 마스킹 처리
record SpaceConfigResponse(
    Long id, Long deptId, String deptName,
    String userEmail,
    String apiToken,   // 항상 "***" 고정
    String spaceKey, String baseUrl,
    String updatedBy, LocalDateTime updatedAt
) {}

// TeamConfigResponse
record TeamConfigResponse(
    Long id, Long teamId, String teamName,
    String parentPageId,
    String updatedBy, LocalDateTime updatedAt
) {}
```

### 2-9. 인가 & IDOR 방어
- 모든 관리 API: `@PreAuthorize("hasRole('ADMIN')")`
- 추가로, 요청자의 `deptId`와 경로 변수 `deptId` 일치 여부 서비스 계층에서 검증
  - 슈퍼 어드민은 전체 접근 허용 (`ROLE_SUPER_ADMIN`)
- 감사 필드 `created_by` / `updated_by`에 현재 로그인 사용자 이메일 자동 기록

---

## 3. 프론트엔드 구조

### 3-1. 라우트
```
/admin/confluence
  ├── SpaceConfigTab    (실별 인증 정보 관리)
  └── TeamConfigTab     (팀별 부모 페이지 ID 관리)
```

### 3-2. 컴포넌트
```
frontend/src/pages/admin/confluence/
├── ConfluenceAdminPage.tsx      (탭 컨테이너)
├── SpaceConfigForm.tsx          (실 선택 + 이메일/토큰/스페이스키 입력 + 연결 테스트 버튼)
├── SpaceConfigTable.tsx         (등록된 실별 설정 목록, api_token "***" 표시)
├── TeamConfigForm.tsx           (팀 선택 + parent_page_id 입력)
├── TeamConfigTable.tsx          (등록된 팀별 경로 목록)
└── hooks/
    ├── useConfluenceConfigs.ts  (Space + Team 통합 훅)
```

### 3-3. 주요 UX 흐름
1. 실 선택 드롭다운(departments API) → 이메일/토큰/스페이스키/Base URL 입력 → **연결 테스트** → 저장
2. 팀 선택 드롭다운(teams API) → 부모 페이지 ID 입력 → 저장
3. 저장 성공 시 테이블 갱신, api_token은 `***` 마스킹 표시
4. 삭제 버튼 → 확인 모달 → DELETE API 호출

---

## 4. 보안 규칙 (구현 시 반드시 준수)
- `api_token` 복호화 값은 **절대 로그 출력 금지** (SLF4J MDC, AOP 로깅 포함)
- HTTPS 강제: Spring Security `requiresSecure()` 또는 리버스 프록시 설정
- 요청 로깅 필터에서 `/api/admin/confluence/spaces` POST 바디 마스킹
- `CONFLUENCE_TOKEN_SECRET` 환경변수 미설정 시 애플리케이션 기동 실패 처리

---

## 5. 작업 순서 및 담당

| 순서 | 담당 | 작업 |
|------|------|------|
| 1 | BE | teams 테이블 dept_id 컬럼 존재 확인 및 마이그레이션 |
| 2 | BE | DB 마이그레이션(Flyway): confluence_space_configs, confluence_team_configs |
| 3 | BE | AesEncryptionService 구현 (IV 포함 GCM 방식) |
| 4 | BE | Entity, Repository 구현 |
| 5 | BE | ConfluenceConfigService (Space + Team 통합) 구현 |
| 6 | BE | ConfluenceConfigController + 응답 DTO 구현 |
| 7 | BE | ConfluenceClientCache 구현 |
| 8 | BE | ConfluenceClient 동적 인증 리팩토링 |
| 9 | BE | ConfluenceUploadService 팀→실 매핑 + 에러 처리 |
| 10 | BE | yml confluence.* 제거 |
| 11 | FE | ConfluenceAdminPage + 탭 구조 구현 |
| 12 | FE | SpaceConfigForm/Table + 연결 테스트 버튼 구현 |
| 13 | FE | TeamConfigForm/Table 구현 |
| 14 | FE | 사이드바 관리 메뉴에 Confluence 설정 항목 추가 |

---

## 6. 체크리스트
- [x] teams.dept_id 컬럼 확인
- [x] DB 마이그레이션 파일 작성
- [x] AES-256-GCM 암호화 서비스 (IV 포함)
- [x] 키 로테이션 배치 로직
- [x] 실별 설정 CRUD API (DELETE 포함)
- [x] 팀별 설정 CRUD API (DELETE 포함)
- [x] IDOR 방어 (소유권 검증)
- [x] Confluence 연결 테스트 API
- [x] Base URL SSRF 화이트리스트 검증
- [x] ConfluenceClientCache (dept별, 무효화 포함)
- [x] 팀→실 매핑 실패 에러 처리 (404/409)
- [x] 응답 DTO api_token 마스킹
- [x] 복호화 토큰 로그 출력 금지 검증
- [x] yml confluence.* 제거
- [x] React 관리 페이지 (SpaceConfig + 연결 테스트)
- [x] React 관리 페이지 (TeamConfig + 삭제)
- [x] 감사 필드 (created_by / updated_by) 자동 기록

---

## Review 결과
- 검토일: 2026-04-20
- 검토 항목: 보안 / 리팩토링 / 기능
- 최초 결과: FAIL (critical 5, major 6, minor 5)
- 수정 반영:
  - [critical] IDOR → 소유권 검증 로직 추가 (2-9)
  - [critical] AES-GCM IV → IV 포함 저장 전략 명시 (2-2)
  - [critical] 키 로테이션 → 레코드 단위 재암호화 배치 명시 (2-2)
  - [critical] DELETE API → 엔드포인트 추가 (2-7)
  - [critical] 팀→실 매핑 실패 처리 → 에러 흐름 명시 (2-3)
  - [major] ConfluenceClient 캐싱 → ConfluenceClientCache 추가 (2-4)
  - [major] GET 응답 DTO → SpaceConfigResponse / TeamConfigResponse 정의 (2-8)
  - [major] 연결 테스트 API → 엔드포인트 추가 (2-7)
  - [major] teams.dept_id 전제 검증 → 마이그레이션 1번 항목 추가
  - [major] 복호화 토큰 로깅 금지 → 보안 규칙 섹션 추가 (4)
  - [minor] Base URL SSRF → 화이트리스트 검증 추가 (2-5)
  - [minor] 감사 로그 → created_by / updated_by 컬럼 추가 (1, 2-9)
  - [minor] SpaceConfigService/TeamConfigService 분리 → ConfluenceConfigService 통합
- 최종 결과: PASS
