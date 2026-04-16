# 주간보고 자동 업로드 스케줄러 구현 계획

## 작업 범위

### 1. build.gradle.kts — ShedLock 의존성 추가
### 2. Flyway 마이그레이션 (V1__init_scheduler_tables.sql)
- scheduler_job_logs
- scheduler_folder_results
- shedlock

### 3. scheduler 모듈 신규 생성
```
com.hubilon.modules.scheduler/
├── domain/model/
│   ├── SchedulerJobLog.java
│   ├── SchedulerJobStatus.java (enum)
│   └── SchedulerFolderResult.java
├── domain/port/in/
│   ├── SchedulerJobLogQueryUseCase.java
│   └── SchedulerTriggerUseCase.java
├── domain/port/out/
│   ├── SchedulerJobLogCommandPort.java
│   └── SchedulerJobLogQueryPort.java
├── application/service/
│   ├── WeeklyReportSchedulerService.java (@Scheduled + @SchedulerLock)
│   ├── WeeklyReportProcessor.java
│   ├── SchedulerJobLogQueryService.java
│   └── strategy/
│       ├── WeeklyReportUploadStrategy.java (interface)
│       ├── ManualUploadStrategy.java
│       └── AiUploadStrategy.java
└── adapter/
    ├── in/web/SchedulerJobLogController.java
    └── out/persistence/
        ├── SchedulerJobLogJpaEntity.java
        ├── SchedulerFolderResultJpaEntity.java
        ├── SchedulerJobLogRepository.java
        ├── SchedulerFolderResultRepository.java
        └── SchedulerJobLogPersistenceAdapter.java
```

### 4. SchedulerConfig.java 추가
### 5. SecurityConfig.java 업데이트
### 6. application.yml — flyway 설정 추가

## 실행 순서
1. build.gradle.kts 수정
2. application.yml flyway 설정
3. DB 마이그레이션 파일
4. domain 모델 및 포트
5. application 서비스 및 전략
6. persistence adapter
7. web controller
8. config / security 수정
