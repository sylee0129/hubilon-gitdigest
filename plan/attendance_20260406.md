# 출퇴근 기능 구현 계획

## 개요

- 메인 페이지 `AttendanceWidget`: 출근/퇴근 버튼 (백엔드 연동)
- 캘린더 페이지: 날짜별 출퇴근 상태 컬러 원형 dot 표시 (마우스 오버 시 출근 시각 툴팁)

---

## 출퇴근 기준

| 상태 | 기준 | 표시 색상 |
|------|------|----------|
| 정상 | 사용자 기준 시각 이전 출근 | 초록 🟢 |
| 지각A | 기준 시각 +1~15분 | 노랑 🟡 |
| 지각B | 기준 시각 +16~30분 | 주황 🟠 |
| 지각C | 기준 시각 +31분 이상 | 빨강 🔴 |
| 결근/미출근 | 당일 출근 기록 없음 | 표시 안 함 |

- **기준 시각**: 사용자별 `work_start_time` (기본값 09:00)
- **status 계산 책임**: `domain` 레이어 순수 Java (프레임워크 의존 없음)
- **캘린더 dot 툴팁**: 마우스 오버 시 출근 시각 표시 (예: `09:31`)

---

## Backend 작업

### 1. `user` 테이블 변경
- `work_start_time TIME DEFAULT '09:00'` 컬럼 추가
- `UserJpaEntity`, `User` 도메인 모델, 관련 DTO에 필드 추가

### 2. 신규 도메인: `attendance`

**DB 테이블: `attendance`**
```
id              BIGINT PK AUTO_INCREMENT
user_id         BIGINT FK → user
check_in_at     DATETIME          출근 시각
check_out_at    DATETIME NULL     퇴근 시각 (퇴근 전 null)
work_date       DATE              근무 일자
status          VARCHAR(10)       NORMAL / LATE_A / LATE_B / LATE_C
created_at      DATETIME
updated_at      DATETIME

UNIQUE (user_id, work_date)
```

**API 엔드포인트** (모든 API JWT 인증 필수, JWT userId로 본인 데이터만 조회)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/attendance/check-in` | 출근 — 중복 출근 시 에러 |
| POST | `/api/v1/attendance/check-out` | 퇴근 — 미출근/이미 퇴근 시 에러 |
| GET | `/api/v1/attendance/me/today` | 오늘 출퇴근 상태 조회 |
| GET | `/api/v1/attendance/me?year=&month=` | 월별 출퇴근 이력 조회 |

**월별 조회 응답 필드**: `workDate`, `checkInAt`, `checkOutAt`, `status`

**비즈니스 로직**
- 출근: 현재 시각 기록, 사용자 `work_start_time`과 비교하여 status 계산 (domain 레이어)
- 퇴근: `check_out_at` 업데이트
- **자동 퇴근 (스케줄러)**: 매일 자정(00:00) 실행 → 당일 `check_out_at` null인 레코드 → `check_in_at + 9시간`으로 자동 퇴근 처리

### 3. Hexagonal Architecture 구조
```
modules/attendance/
├── domain/
│   ├── model/Attendance.java          # 도메인 모델 (status 계산 포함)
│   ├── model/AttendanceStatus.java    # enum: NORMAL, LATE_A, LATE_B, LATE_C
│   ├── port/in/                       # AttendanceCheckInUseCase 등
│   └── port/out/                      # AttendanceCommandPort, AttendanceQueryPort
├── application/
│   ├── service/command/               # CheckInService, CheckOutService
│   ├── service/query/                 # AttendanceMeQueryService
│   └── dto/                           # Command / Result DTO
└── adapter/
    ├── in/web/                        # AttendanceController
    └── out/persistence/               # JPA Entity, Repository, Adapter
```

---

## Frontend 작업

### 1. `attendanceService.ts` 신규 생성
- `checkIn()`, `checkOut()`
- `getTodayAttendance()` → `{ checkInAt, checkOutAt, status }`
- `getMonthlyAttendance(year, month)` → `AttendanceRecord[]`

### 2. `useAttendance.ts` 커스텀 훅 신규 생성
- 오늘 출퇴근 상태 관리
- 출근/퇴근 핸들러

### 3. `AttendanceWidget.tsx` 수정 (목업 → 실제 연동)
- 미출근: **"출근"** 버튼
- 출근 후 퇴근 전: **"퇴근"** 버튼 + 출근 시각 + 경과 시간 (프론트 실시간 계산)
- 퇴근 후: 출근/퇴근 시각 표시, 버튼 비활성화

### 4. `CalendarPage.tsx` 수정
- 월 변경 시 `getMonthlyAttendance()` 호출
- 날짜 셀에 status별 컬러 dot 표시
- **툴팁**: 마우스 오버 시 `checkInAt` 시각 표시 (예: `09:31`)

---

## 작업 순서

1. BE: `user` 테이블 `work_start_time` 컬럼 추가 및 관련 코드 수정
2. BE: `attendance` 도메인 전체 구현
3. BE: 자동 퇴근 스케줄러 구현
4. FE: `attendanceService.ts`, `useAttendance.ts` 구현
5. FE: `AttendanceWidget.tsx` 실제 연동
6. FE: `CalendarPage.tsx` dot + 툴팁 표시

---

## 비고

- 주말/공휴일 처리는 이번 범위 제외
- 관리자 출퇴근 조회는 이번 범위 제외

---

## Review 결과
- 검토일: 2026-04-06
- 검토 항목: 보안 / 리팩토링 / 기능
- 확정 사항: 사용자별 work_start_time DB 저장, 자정 자동 퇴근(check_in_at + 9h)
- 결과: 이상 없음 (구현 진행)
