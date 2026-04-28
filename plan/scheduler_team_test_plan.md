# 스케줄러 팀 단위 고도화 — 테스트 계획

## 날짜
2026-04-22

## 목표
스케줄러 팀 단위 고도화 구현 후 단위 테스트 작성 및 실행

## 백엔드 테스트
- 대상 클래스: SchedulerTeamConfig, SchedulerTeamConfigService, SchedulerJobLog, WeeklyReportSchedulerService
- 핵심 테스트:
  - SchedulerTeamConfigService.upsert() — 신규/업데이트 케이스
  - SchedulerJobLog.createRunning(teamId, teamName) — 필드 검증

## 프론트엔드 테스트
- package.json test 스크립트 확인
- 없으면 npm run build 확인

## 진행 상태
- [ ] 기존 테스트 확인
- [ ] 테스트 작성
- [ ] gradlew test 실행
- [ ] 프론트엔드 확인
