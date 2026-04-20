# 팀 기반 폴더 필터링 구현 Plan

## 작업 목록

### BE-1: FolderJpaEntity에 team 관계 추가
- FolderJpaEntity에 TeamJpaEntity ManyToOne 관계 추가
- Builder에 team 파라미터 추가
- updateTeam() 메서드 추가
- Folder 도메인에 teamId 필드 추가

### BE-2: 폴더 생성/수정 Request/Command/Service에 teamId 추가
- FolderCreateRequest, FolderUpdateRequest에 teamId 추가
- FolderCreateCommand, FolderUpdateCommand에 teamId 추가
- FolderWebMapper 매핑 추가
- FolderPersistenceAdapter에서 team 설정 (TeamRepository 직접 주입)

### BE-3: SecurityUtils 공통 유틸 신규 생성

### BE-4: 팀별 폴더 조회 서버사이드 필터링
- FolderJpaRepository 쿼리 메서드 추가
- FolderQueryPort, FolderQueryUseCase 시그니처 변경 (teamId 추가)
- FolderPersistenceAdapter, FolderQueryService 구현 변경
- FolderController에 SecurityUtils 주입, ADMIN/USER 분기 처리

### BE-5: 로그인 응답에 User 정보 포함
- AuthController 확인 결과: 이미 LoginResponse에 UserInfo 포함되어 있음
- LoginResult 레코드 신규 생성 필요 없음 (이미 구현됨)
- LoginUseCase 반환 타입 변경 불필요 (Controller에서 직접 조합)

## 상태: 진행중
