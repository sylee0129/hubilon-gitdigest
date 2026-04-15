# DB Migration Plan: H2 → MariaDB

## Context
개발 환경 DB를 내장형 H2에서 외부 Docker MariaDB로 전환.
접속 대상: `192.168.10.30:3307/gitdigest` (hubilon/hubilon123!@#)

---

## 변경 파일 목록

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | H2 제거, MariaDB 드라이버 추가 |
| `backend/src/main/resources/application.yml` | H2 콘솔 제거, dialect 변경 |
| `backend/src/main/resources/application-dev.yml` | datasource MariaDB로 변경 |

---

## 1. build.gradle.kts

```kotlin
// 제거
runtimeOnly("com.h2database:h2")

// 추가
runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
```

---

## 2. application.yml

```yaml
spring:
  datasource:
    url: jdbc:mariadb://192.168.10.30:3307/gitdigest
    driver-class-name: org.mariadb.jdbc.Driver
    username: hubilon
    password: hubilon123!@#
  # h2.console 블록 전체 제거
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MariaDBDialect
        jdbc:
          time_zone: Asia/Seoul
```

---

## 3. application-dev.yml

```yaml
server:
  port: 8090

spring:
  datasource:
    url: jdbc:mariadb://192.168.10.30:3307/gitdigest
    driver-class-name: org.mariadb.jdbc.Driver
    username: hubilon
    password: hubilon123!@#
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        dialect: org.hibernate.dialect.MariaDBDialect
        jdbc:
          time_zone: Asia/Seoul
```

---

## 주의사항

- `GenerationType.IDENTITY` 전략은 MariaDB AUTO_INCREMENT와 호환 — 변경 불필요
- `@CreatedDate`, `@LastModifiedDate` Auditing 설정 변경 불필요
- Flyway/Liquibase 미사용 → `ddl-auto: update` 유지 (스키마 자동 생성)
- 기존 H2 데이터 파일(`backend/data/worklogai.*`)은 별도 마이그레이션 불필요 (새 DB 사용)

---

## 검증

1. `./gradlew bootRun` 실행 후 MariaDB 연결 성공 확인
2. 테이블 자동 생성 로그 확인
3. 로그인 API 호출하여 정상 동작 확인

## Review 결과
- 검토일: 2026-04-15
- 검토 항목: 보안 / 리팩토링 / 기능
- 반영 사항:
  - 크리덴셜 환경변수 분리 (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`)
  - Dialect 자동 감지 (명시 제거)
  - H2 테스트 환경 영향도 확인 후 처리
