# 스케줄러 크론 표현식 yml 동적 관리

## 목적
`@Scheduled`에 하드코딩된 크론 표현식을 yml로 분리하여 재배포 없이 실행 시간 변경 가능하게.

---

## 변경 파일

### 1. `WeeklyReportSchedulerService.java`
`backend/src/main/java/com/hubilon/modules/scheduler/application/service/WeeklyReportSchedulerService.java`

```java
// 변경 전
@Scheduled(cron = "0 0 19 * * THU", zone = "Asia/Seoul")

// 변경 후
@Scheduled(cron = "${scheduler.weekly-report.cron:0 0 19 * * THU}", zone = "Asia/Seoul")
```

### 2. `application.yml`
`backend/src/main/resources/application.yml`

```yaml
scheduler:
  weekly-report:
    enabled: false
    cron: "0 0 19 * * THU"
```

### 3. `application-dev.yml` (소스)
`backend/src/main/resources/application-dev.yml`

```yaml
scheduler:
  weekly-report:
    enabled: true
    cron: "0 0 19 * * THU"
```

### 4. `deploy/backend/application-dev.yml` (서버)
```yaml
scheduler:
  weekly-report:
    enabled: true
    cron: "0 0 19 * * THU"   # 이 값만 수정 후 서버 재시작하면 반영
```
