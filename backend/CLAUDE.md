# backend — Claude Agent Guide

## 프로젝트 개요

Hubilon groupware

- **베이스 패키지**: `com.hubilon`
- **아키텍처**: Hexagonal
- **ORM**: JPA + QueryDSL

---

## 기술 스택

| 항목 | 버전/설명 |
|------|-----------|
| Java | 25 |
| Spring Boot | 4.0.5 |
| ORM | JPA + QueryDSL |
| API 문서 | SpringDoc OpenAPI (Swagger) |

---

## 패키지 구조

베이스 패키지: `com.hubilon`

### Hexagonal Architecture

```
com.example.demo/
├── modules/{domain}/
│   ├── domain/
│   │   ├── model/               # 도메인 모델 (JPA Entity 아님)
│   │   ├── service/             # 도메인 서비스
│   │   ├── port/
│   │   │   ├── in/              # 인바운드 포트 — UseCase 인터페이스
│   │   │   └── out/             # 아웃바운드 포트 — CommandPort, QueryPort
│   │   └── search/              # SearchFilter
│   ├── application/
│   │   ├── service/
│   │   │   ├── command/         # Register, Modify, Delete 서비스 (UseCase 구현)
│   │   │   └── query/           # Search 서비스 (UseCase 구현)
│   │   ├── dto/                 # Command / Query / Result DTO
│   │   └── mapper/              # ApplicationMapper (MapStruct)
│   └── adapter/
│       ├── in/web/              # Controller, Request/Response Record, WebMapper
│       └── out/persistence/     # JPA Entity, Repository, MapStructMapper, Adapter 구현체
└── common/                      # 공통 모듈 (response, exception, page 등)
```

---

## 공통 패키지

| 역할 | 패키지 경로 |
|------|-------------|
| Response 래퍼 | `common.response` |
| 페이지네이션 | `common.page` |
| 서비스 예외 | `common.exception.custom` |
| 전역 예외 핸들러 | `common.exception.handler` |

---

## 코딩 컨벤션

### 네이밍 규칙

| 유형 | 패턴 | 예시 |
|------|------|------|
| 도메인 모델 | `{Domain}` | `User` |
| JPA 엔티티 | `{Domain}JpaEntity` | `UserJpaEntity` |
| 인바운드 포트 (UseCase) | `{Domain}{Action}UseCase` | `UserRegisterUseCase` |
| 아웃바운드 포트 | `{Domain}CommandPort` / `{Domain}QueryPort` | `UserCommandPort` |
| Command 서비스 | `{Domain}{Action}Service` | `UserRegisterService` |
| Query 서비스 | `{Domain}SearchService` | `UserSearchService` |
| Controller | `{Domain}Controller` | `UserController` |
| Command DTO | `{Domain}{Action}Command` | `UserRegisterCommand` |
| Result DTO | `{Domain}{Action}Result` | `UserRegisterResult` |
| Request Record | `{Domain}{Action}Request` | `UserRegisterRequest` |
| Response Record | `{Domain}{Action}Response` | `UserRegisterResponse` |
| MapStruct Mapper | `{Domain}MapstructMapper` | `UserMapstructMapper` |
| Application Mapper | `{Domain}AppMapper` | `UserAppMapper` |

### Lombok 규칙

**허용:**
| 어노테이션 | 적용 위치 |
|-----------|----------|
| `@Getter` | JPA Entity 필드 접근자 |
| `@Builder` | Entity / 도메인 모델 생성 |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | JPA 기본 생성자 |
| `@RequiredArgsConstructor` | Service / Component 생성자 주입 |
| `@Slf4j` | 로거 |

**금지:**
| 어노테이션 | 이유 |
|-----------|------|
| `@Data` | `equals`/`hashCode` 오버라이드로 JPA 연관관계 무한루프 위험 |
| `@Setter` | 엔티티 불변성 훼손 — 변경은 도메인 메서드로 처리 |
| `@ToString` | 연관관계 포함 시 `LazyInitializationException` 위험 |

**record로 대체 가능한 곳에는 Lombok 사용 금지** — DTO, Command, Query, Result는 record 우선.

### 응답 포맷
- 모든 API 응답은 `common.response`의 Response 래퍼 클래스를 사용한다.
- 페이지네이션 응답은 `common.page`의 PageResult를 사용한다.
- 예외는 `common.exception.custom`의 ServiceException을 상속해 도메인별로 정의한다.
- JPA Entity를 Controller에서 직접 반환하지 않는다. 반드시 Response Record로 변환한다.
- 모든 Controller에 `@Tag`, `@Operation` Swagger 어노테이션을 추가한다.
- QueryDSL 동적 쿼리는 `QueryDsl` 접두사 Repository 구현체에서만 작성한다.

---

## 의존성 방향 (엄격히 준수)

```
adapter/in  →  application  →  domain
adapter/out ←  application  ←  domain (포트를 통해)
```

| 레이어 | 의존 가능 대상 |
|--------|---------------|
| `domain` | 없음 — 순수 Java만 허용, 프레임워크 import 금지 |
| `application` | `domain` 레이어만 (UseCase 인터페이스 구현, Port 인터페이스 호출) |
| `adapter/in` (web) | `domain/port/in` UseCase 인터페이스 주입 |
| `adapter/out` (persistence) | `domain/port/out` Port 인터페이스 구현, JPA |

> 역방향 의존성이 생기면 설계를 재검토한다. application이 adapter를 알아서는 안 된다.

---

## 레이어별 코딩 규칙

### domain/model
- 프레임워크 어노테이션 없음 (`@Entity`, `@Column` 등 금지)
- Lombok: `@Getter @Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 복합키: `{Domain}Id` 클래스 별도 정의 (`Serializable` 구현, `serialVersionUID` 선언)

### domain/port/in (UseCase 인터페이스)
- 인터페이스명: `{Domain}{Action}UseCase` (예: `UserRegisterUseCase`)
- Command: `{Action}(Command command)` — register, modify는 Command DTO 파라미터
- Delete: `delete(pk...)` — PK 필드 직접 파라미터
- Query: `searchDetail(pk...)`, `searchList(query, pageRequest)`, `searchGrid(query, pageRequest)`
- application DTO를 파라미터/반환 타입으로 사용

### domain/port/out
- `{Domain}CommandPort` — 저장/삭제 연산만
- `{Domain}QueryPort` — 조회 연산만
- `SearchFilter`는 `record + @Builder` 로 정의

### application/service
- **command 서비스**: 클래스당 유스케이스 1개, 대응하는 UseCase 인터페이스를 `implements`
- **query 서비스**: `@Transactional(readOnly = true)` **필수** — 생략 불가, UseCase 인터페이스를 `implements`
- AppMapper의 `updateDomain()` 메서드에 `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` 적용

### application/dto
- 모두 `record` 사용
- validation 어노테이션(`@NotNull`, `@Size` 등) 금지 — web 레이어(Request record) 책임

### adapter/in/web (Controller)
- **Service 클래스가 아닌 UseCase 인터페이스를 주입**받는다
- Request record에 validation 어노테이션 위치
- Response record에 `@JsonFormat` 위치

### adapter/out/persistence
- `{Domain}CommandAdapter` (`@Component`) — CommandPort 구현
  - `save(domain)` 구현 시 `saveAndFlush` 사용 — `@LastModifiedDate` 등 감사 필드가 반환값에 즉시 반영
- `{Domain}QueryAdapter` (`@Component`) — QueryPort 구현, find/exists/page
- `{Domain}JpaEntity` — `@Builder(toBuilder = true)` 적용, `@Comment`로 컬럼 설명
- `QueryDslRepositoryImpl` — JPAQueryFactory 사용, 동적 쿼리 전담

---

## 예외 처리

- `RuntimeException`, `Exception` 최상위 타입 직접 사용 금지 — 도메인 예외 계층 정의 후 사용
- 도메인 예외 계층 구조 (예시):
  ```
  BaseException (abstract)
  ├── NotFoundException       # 404 — EntityNotFoundException 등
  ├── ForbiddenException      # 403 — 소유자 외 접근
  ├── InvalidRequestException # 400 — 비즈니스 규칙 위반
  └── ExternalServiceException # 502 — 외부 API 오류
  ```
- `@RestControllerAdvice`에서 각 예외 타입을 HTTP 상태 코드로 매핑
- `catch` 블록에서 예외를 삼키는 것 금지 — 최소 `log.warn()` 또는 재던지기

---

## 새 도메인 모듈 추가 체크리스트

Auto-Code 앱으로 코드를 생성한 뒤 아래 순서로 연결한다.

- [ ] `domain/model/` — 도메인 모델
- [ ] `domain/port/in/` — Register / Modify / Delete / Search UseCase 인터페이스
- [ ] `domain/port/out/` — CommandPort, QueryPort 인터페이스
- [ ] `application/dto/` — Command / Query / Result DTO
- [ ] `application/service/command/` — Register, Modify, Delete 서비스 (UseCase implements)
- [ ] `application/service/query/` — Search 서비스 (UseCase implements)
- [ ] `application/mapper/` — ApplicationMapper
- [ ] `adapter/in/web/` — Controller (UseCase 주입) + Request/Response Record + WebMapper
- [ ] `adapter/out/persistence/` — JPA Entity, Repository, MapStructMapper, Adapter 구현체

---

## 주의사항

- 새 기능 추가 전 기존 모듈의 패턴을 먼저 파악하고 일관성을 유지한다.
- 커밋 메시지는 브랜치 전략에 따른 컨벤션을 준수한다.
- 성능에 영향을 줄 수 있는 쿼리 변경 시 실행 계획(EXPLAIN)을 확인한다.
