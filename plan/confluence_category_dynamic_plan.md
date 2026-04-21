# Confluence 카테고리 동적 로드 — Plan

## 문제
- FE는 `categoryId(number)` + `categoryName(string)` 전송
- BE `WeeklyReportRowDto`는 `String category`만 존재 → JSON 매핑 불일치 → `category` 항상 null
- `CATEGORY_ORDER = Map.of(...)` 에 null 키 조회 시 NPE (`ImmutableCollections$MapN.probe`)
- `CATEGORY_ORDER` / `CATEGORY_LABEL` 하드코딩으로 DB 카테고리 변경 미반영

## 목표
- `WeeklyReportRowDto` FE 전송값과 일치 (categoryId, categoryName)
- 정렬·레이블을 DB `categories.sort_order`, `categories.name`에서 동적 로드

---

## 1. 수정 파일

| 파일 | 변경 유형 |
|------|----------|
| `backend/.../confluence/adapter/in/web/WeeklyConfluenceRequest.java` | DTO 필드 변경 |
| `backend/.../confluence/application/service/ConfluenceWeeklyReportService.java` | 하드코딩 제거, 동적 로드 |

---

## 2. 수정 내용

### WeeklyConfluenceRequest.java — WeeklyReportRowDto

```java
// 변경 전
public record WeeklyReportRowDto(
    String category,
    String folderName,
    List<String> members,
    String progressSummary,
    String planSummary
) {}

// 변경 후
public record WeeklyReportRowDto(
    Long categoryId,
    String categoryName,
    String folderName,
    List<String> members,
    String progressSummary,
    String planSummary
) {}
```

### ConfluenceWeeklyReportService.java

**제거:**
```java
private static final Map<String, Integer> CATEGORY_ORDER = Map.of(...);
private static final Map<String, String> CATEGORY_LABEL = Map.of(...);
```

**의존성 추가:**
```java
private final CategoryQueryPort categoryQueryPort;
// CategoryPersistenceAdapter가 이미 구현체 제공 (findAllOrderBySortOrder 포함)
```

**upload() — 카테고리 동적 로드 추가:**
```java
List<Category> categories = categoryQueryPort.findAllOrderBySortOrder();
Map<Long, Integer> categoryOrder = categories.stream()
    .collect(Collectors.toMap(Category::getId, Category::getSortOrder));
Map<Long, String> categoryLabel = categories.stream()
    .collect(Collectors.toMap(Category::getId, Category::getName));
// → buildXhtml()에 전달
```

**buildXhtml() 시그니처 변경:**
```java
private String buildXhtml(List<WeeklyReportRowDto> rows, LocalDate startDate, LocalDate endDate,
                           Map<Long, Integer> categoryOrder, Map<Long, String> categoryLabel)
```

**정렬 (null-safe):**
```java
List<WeeklyReportRowDto> sorted = rows.stream()
    .sorted(Comparator.comparingInt(r ->
        r.categoryId() != null ? categoryOrder.getOrDefault(r.categoryId(), 99) : 99))
    .toList();
```

**그룹핑 — String → Long 키:**
```java
Map<Long, List<WeeklyReportRowDto>> grouped = new LinkedHashMap<>();
for (WeeklyReportRowDto row : sorted) {
    Long key = row.categoryId() != null ? row.categoryId() : -1L;
    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
}
```

**레이블:**
```java
String label = row.categoryId() != null
    ? categoryLabel.getOrDefault(row.categoryId(),
        row.categoryName() != null ? row.categoryName() : "기타")
    : "기타";
```

---

## 3. 체크리스트

- [ ] `WeeklyReportRowDto` 필드 변경 (category → categoryId, categoryName)
- [ ] `CATEGORY_ORDER` / `CATEGORY_LABEL` 정적 맵 제거
- [ ] `CategoryQueryPort` 의존성 주입
- [ ] `upload()` 카테고리 동적 로드
- [ ] `buildXhtml()` 파라미터·정렬·그룹핑·레이블 변경
- [ ] 빌드 확인 (`./gradlew build`)

---

## 4. 검증

1. 백엔드 재기동
2. Confluence 업로드 버튼 클릭 → 500 에러 없이 페이지 생성/수정 성공
3. 카테고리 정렬 순서가 DB `categories.sort_order` 기준 출력
4. 새 카테고리 추가 후 업로드 시 자동 반영

## Review 결과
- 검토일: 2026-04-21
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 → 구현 범위에 포함

### 추가 수정 항목
- [F-3] 스케줄러 파일(AiUploadStrategy, ManualUploadStrategy, WeeklyReportProcessor 등) DTO 변경 반영
- [F-4] ConfluenceWeeklyReportServiceTest 등 테스트 파일 수정
- [F-1] categoryId=null 테스트 케이스 추가
- [F-2] FE null 직렬화 확인 (categoryId: null 전송 시 BE에서 정상 처리)
- [R-3] buildXhtml() 파라미터를 CategoryMeta record로 묶기
- [R-2] spaceConfigRepository.findByDeptId() 중복 쿼리 제거
