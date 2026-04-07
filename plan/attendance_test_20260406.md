# Attendance 기능 테스트 계획 (2026-04-06)

## 목표
출퇴근(attendance) 도메인의 핵심 로직에 대한 단위 테스트 작성 및 전체 테스트 실행

## 테스트 작성 대상

### 1. `Attendance.java` — `calculateStatus()` 도메인 메서드
- 기준 시각 이전/정각: NORMAL
- +1~15분 지각: LATE_A
- +16~30분 지각: LATE_B
- +31분 이상 지각: LATE_C
- 경계값(0, 1, 15, 16, 30, 31분) 케이스

### 2. `AttendanceCheckInService.java`
- 정상 출근 처리
- 중복 출근 시 `InvalidRequestException` 발생

### 3. `AttendanceCheckOutService.java`
- 정상 퇴근 처리
- 미출근 시 `NotFoundException` 발생
- 이미 퇴근 시 `InvalidRequestException` 발생

## 실행 순서
1. 테스트 파일 작성
2. `./gradlew test` 실행
3. 실패 시 원인 파악 후 수정 (최대 3회)

## 결과 보고 형식
- 성공: "✅ attendance 구현 및 테스트 완료 (백엔드 N건 통과 / 프론트엔드 테스트 환경 없음)"
- 실패: 실패 내용과 원인 보고
