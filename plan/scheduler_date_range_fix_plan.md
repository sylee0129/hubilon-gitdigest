# 주간보고 날짜 범위 수정 계획

## Context
현재 스케줄러는 월~일(7일)을 startDate/endDate로 사용한다.
이 날짜가 Confluence 컬럼 헤더(금주/차주 날짜 표시), 주간보고 헤더(금주/차주 날짜 표시)와 git 커밋 조회 양쪽에 동일하게 전달된다.

요구사항:
1. 컬럼 헤더 날짜를 월~금(업무일)으로 표시
2. git 커밋 조회 범위를 전주 토요일~금요일로 확장 (주말 커밋 포함)

---

## 변경 파일

- `backend/src/main/java/com/hubilon/modules/scheduler/application/service/WeeklyReportSchedulerService.java`

---

## 구현 내용

### WeeklyReportSchedulerService.triggerForTeam()

**현재**
```java
LocalDate startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
LocalDate endDate = startDate.plusDays(6); // 일요일

// git 조회 + Confluence 헤더 모두 startDate(월)~endDate(일) 사용
weeklyReportProcessor.process(folder, startDate, endDate);
new WeeklyConfluenceRequest(teamId, successRows, startDate.toString(), endDate.toString());
```

**변경 후**
```java
LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

LocalDate gitStartDate = monday.minusDays(2);  // 전주 토요일
LocalDate gitEndDate   = monday.plusDays(4);   // 금요일

// git 커밋 조회: 전주 토~금
weeklyReportProcessor.process(folder, gitStartDate, gitEndDate);

// Confluence 헤더: 월~금 → "금주 진행사항 (4/20~4/25)"
new WeeklyConfluenceRequest(teamId, successRows, monday.toString(), gitEndDate.toString());
```

### ConfluenceWeeklyReportService.buildXhtml()

변경 불필요.
- startDate(월), endDate(금) 수신 시 헤더 자동으로 "4/20~4/24" 표시
- 차주 계산 `startDate.plusDays(7)` ~ `endDate.plusDays(7)` = 다음주 월~금 ✓

---

## 날짜 예시 (이번 주 2026-04-20 기준)

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| git 조회 범위 | 4/20(월) ~ 4/26(일) | 4/18(토) ~ 4/24(금) |
| 금주 헤더 표시 | 4/20 ~ 4/26 | 4/20 ~ 4/24 |
| 차주 헤더 표시 | 4/27 ~ 5/3 | 4/27 ~ 5/1 |

---

## 검증

1. 백엔드 재시작 후 수동 실행
2. Confluence 페이지 확인:
   - 컬럼 헤더: "금주 진행사항 (4/20~4/25)" (일요일 제외)
   - "차주 진행계획 (4/27~5/2)" (일요일 제외)
3. 전주 토요일 커밋이 포함되는지 확인
4. `./gradlew test` 통과 확인