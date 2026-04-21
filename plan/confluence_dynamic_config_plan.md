# Confluence 연동 동적 관리 기능 구현 계획

## 사전 확인 결과
1. `teams` 테이블 → `TeamJpaEntity`에 `deptId` 컬럼 존재 (BIGINT, NOT NULL)
2. 기존 Confluence 코드:
   - `ConfluenceProperties` (ConfigurationProperties)
   - `ConfluenceApiClient` (RestClient 기반)
   - `ConfluenceWeeklyReportService` (ConfluenceProperties, ConfluenceApiClient 의존)
   - `application.yml`에 `confluence.*` 설정 존재
3. DB 마이그레이션: Flyway 사용 (`db/migration/`), 현재 V4까지 존재

## 구현 파일 목록

### 신규 생성
- `V5__confluence_config_tables.sql` — confluence_space_configs, confluence_team_configs 테이블 생성
- `AesEncryptionService` — AES-256-GCM 암복호화
- `ConfluenceSpaceConfigJpaEntity` — space config Entity
- `ConfluenceTeamConfigJpaEntity` — team config Entity
- `ConfluenceSpaceConfigRepository` — JPA Repository
- `ConfluenceTeamConfigRepository` — JPA Repository
- `ConfluenceConfigService` — Space + Team 통합 서비스
- `ConfluenceClientCache` — ConcurrentHashMap 기반 캐시
- `ConfluenceConfigController` — `/api/admin/confluence` 엔드포인트

### 수정
- `ConfluenceApiClient` — baseUrl/credentials를 생성자 파라미터로 받도록 리팩토링
- `ConfluenceWeeklyReportService` — ConfluenceClientCache 기반으로 변경
- `application.yml` — `confluence.*` 설정 제거
- `ConfluenceProperties` — 삭제 (또는 미사용으로 방치)

## 패키지 구조
```
modules/confluence/
├── adapter/
│   ├── in/web/
│   │   ├── ConfluenceController.java (기존)
│   │   ├── ConfluenceConfigController.java (신규)
│   │   ├── SpaceConfigUpsertRequest.java (신규)
│   │   └── TeamConfigUpsertRequest.java (신규)
│   └── out/
│       ├── external/
│       │   └── ConfluenceApiClient.java (수정)
│       └── persistence/
│           ├── ConfluenceSpaceConfigJpaEntity.java (신규)
│           ├── ConfluenceTeamConfigJpaEntity.java (신규)
│           ├── ConfluenceSpaceConfigRepository.java (신규)
│           └── ConfluenceTeamConfigRepository.java (신규)
├── application/
│   ├── dto/
│   │   ├── SpaceConfigResponse.java (신규)
│   │   └── TeamConfigResponse.java (신규)
│   └── service/
│       ├── AesEncryptionService.java (신규)
│       ├── ConfluenceClientCache.java (신규)
│       ├── ConfluenceConfigService.java (신규)
│       └── ConfluenceWeeklyReportService.java (수정)
└── config/
    └── ConfluenceProperties.java (삭제 예정)
```
