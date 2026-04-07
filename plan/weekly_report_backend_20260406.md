# 주간보고 관리 백엔드 구현 계획

## 작업일: 2026-04-06

## 개요
주간보고 관리 백엔드를 처음부터 구현한다.
- Java 25 + Spring Boot 4.0.5
- Hexagonal Architecture
- JPA + H2 (인메모리 DB)
- Apache POI (엑셀 생성)
- WebClient (GitLab API 호출)

---

## 구현 순서

### 1단계: 프로젝트 빌드 파일
- [ ] settings.gradle.kts
- [ ] build.gradle.kts
- [ ] src/main/resources/application.yml

### 2단계: common 패키지
- [ ] common/response/Response.java (record)
- [ ] common/page/PageResult.java (record)
- [ ] common/exception/ServiceException.java (abstract)
- [ ] common/exception/NotFoundException.java
- [ ] common/exception/InvalidRequestException.java
- [ ] common/exception/ExternalServiceException.java
- [ ] common/exception/GlobalExceptionHandler.java
- [ ] common/config/WebConfig.java (CORS)
- [ ] common/config/WebClientConfig.java

### 3단계: project 모듈
- [ ] domain/model/Project.java
- [ ] domain/port/in/ProjectRegisterUseCase.java
- [ ] domain/port/in/ProjectDeleteUseCase.java
- [ ] domain/port/in/ProjectSearchUseCase.java
- [ ] domain/port/out/ProjectCommandPort.java
- [ ] domain/port/out/ProjectQueryPort.java
- [ ] application/dto/ProjectRegisterCommand.java (record)
- [ ] application/dto/ProjectRegisterResult.java (record)
- [ ] application/dto/ProjectSearchResult.java (record)
- [ ] application/service/command/ProjectRegisterService.java
- [ ] application/service/command/ProjectDeleteService.java
- [ ] application/service/query/ProjectSearchService.java
- [ ] application/mapper/ProjectAppMapper.java
- [ ] adapter/in/web/ProjectController.java
- [ ] adapter/in/web/ProjectRegisterRequest.java (record)
- [ ] adapter/in/web/ProjectRegisterResponse.java (record)
- [ ] adapter/in/web/ProjectWebMapper.java
- [ ] adapter/out/persistence/ProjectJpaEntity.java
- [ ] adapter/out/persistence/ProjectJpaRepository.java
- [ ] adapter/out/persistence/ProjectPersistenceAdapter.java (Command + Query)

### 4단계: report 모듈
- [ ] domain/model/Report.java
- [ ] domain/model/CommitInfo.java
- [ ] domain/model/FileChange.java
- [ ] domain/port/in/ReportAnalyzeUseCase.java
- [ ] domain/port/in/ReportSummaryUpdateUseCase.java
- [ ] domain/port/in/ReportExportUseCase.java
- [ ] domain/port/in/ReportSearchUseCase.java
- [ ] domain/port/out/ReportCommandPort.java
- [ ] domain/port/out/ReportQueryPort.java
- [ ] domain/port/out/GitLabPort.java
- [ ] domain/port/out/AiSummaryPort.java
- [ ] application/dto/ (Command/Query/Result records)
- [ ] application/service/ReportAnalyzeService.java
- [ ] application/service/ReportSummaryUpdateService.java
- [ ] application/service/ReportExportService.java
- [ ] application/service/ReportSearchService.java
- [ ] adapter/in/web/ReportController.java
- [ ] adapter/out/persistence/ (JPA entities + adapters)
- [ ] adapter/out/gitlab/GitLabAdapter.java
- [ ] adapter/out/ai/AiSummaryAdapter.java (stub)

### 5단계: Main Application 클래스
- [ ] WorkLogAiApplication.java

---

## API 엔드포인트 요약

| Method | Path | 설명 |
|--------|------|------|
| GET | /api/projects | 프로젝트 목록 |
| POST | /api/projects | GitLab 프로젝트 등록 |
| DELETE | /api/projects/{id} | 프로젝트 삭제 |
| GET | /api/reports | 주간 분석 결과 조회 |
| PUT | /api/reports/{reportId}/summary | 요약 수동 편집 |
| GET | /api/reports/export | 엑셀 내보내기 |

---

## 주요 설계 결정

1. ProjectPersistenceAdapter가 CommandPort와 QueryPort 모두 구현 (단순 도메인)
2. Report 도메인은 Command/Query 어댑터 분리
3. GitLab API 연동: WebClient + PAT/OAuth 지원
4. AI 요약 stub: 커밋 메시지를 "• {message}" 형태로 조합
5. 엑셀: "전체 요약" + 프로젝트별 시트 구성
6. 보고서 캐싱: DB에 존재하면 재사용 (수동 편집본 우선)
