# Plan: 카테고리 DB 테이블 관리

## Context
현재 폴더(사업)의 카테고리(개발사업/신규추진사업/기타)가 Java enum(`FolderCategory`)으로 하드코딩되어 있음.
사용자 요청: DB 테이블로 관리 + 사이드바 "카테고리 추가" 버튼으로 동적 추가/관리.

---

## 변경 범위

### 백엔드

#### 1. Flyway 마이그레이션 (V3)
```sql
-- categories 테이블 생성
CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 기존 enum 값 이관
INSERT INTO categories (name, sort_order) VALUES ('개발사업', 0), ('신규추진사업', 1), ('기타', 2);

-- folders 테이블에 category_id 컬럼 추가
ALTER TABLE folders ADD COLUMN category_id BIGINT;

-- 기존 category 값 → category_id 매핑
UPDATE folders SET category_id = 1 WHERE category = 'DEVELOPMENT';
UPDATE folders SET category_id = 2 WHERE category = 'NEW_BUSINESS';
UPDATE folders SET category_id = 3 WHERE category = 'OTHER';

-- NOT NULL + FK 설정
ALTER TABLE folders MODIFY COLUMN category_id BIGINT NOT NULL;
ALTER TABLE folders ADD CONSTRAINT fk_folders_category FOREIGN KEY (category_id) REFERENCES categories(id);

-- 기존 category 컬럼 제거
ALTER TABLE folders DROP COLUMN category;
```

#### 2. 신규 Category 도메인/엔티티
- `Category.java` (도메인 모델): id, name, sortOrder
- `CategoryJpaEntity.java`: `@Entity @Table(name = "categories")`
- `CategoryJpaRepository.java`: `JpaRepository<CategoryJpaEntity, Long>`
- `CategoryController.java`: `/api/categories`
  - `GET /api/categories` — 전체 조회 (인증 불필요)
  - `POST /api/categories` — 생성 (`ADMIN` 권한 필요)
  - `PUT /api/categories/{id}` — 수정 (`ADMIN` 권한 필요)
  - DELETE 엔드포인트 없음 (카테고리 삭제 불가 정책)

#### 3. Folder 도메인 수정
- `Folder.java`: `FolderCategory category` → `Long categoryId`
- `FolderJpaEntity.java`: `@Enumerated` 제거, `@ManyToOne @JoinColumn(name = "category_id")` 추가
- `FolderCreateRequest/UpdateRequest.java`: `FolderCategory category` → `Long categoryId`
- `FolderResponse.java`: `categoryId` + `categoryName` 반환
- `FolderCreateCommand/UpdateCommand.java`: `categoryId` 로 변경
- `FolderResult.java`: `categoryId`, `categoryName` 포함
- `FolderPersistenceAdapter.java`: 매핑 로직 수정
- `FolderCategory.java`: 삭제

---

### 프론트엔드

#### 1. 타입 변경 (`types/folder.ts`)
```ts
// 기존 제거
type FolderCategory = 'DEVELOPMENT' | 'NEW_BUSINESS' | 'OTHER'
const CATEGORY_LABELS = { ... }

// 추가
interface Category {
  id: number
  name: string
  sortOrder: number
}
```

#### 2. Category API (`services/categoryApi.ts` 신규)
```ts
getCategories(): Promise<Category[]>
createCategory(name: string): Promise<Category>
updateCategory(id: number, name: string): Promise<Category>
deleteCategory(id: number): Promise<void>
```

#### 3. useCategories hook (`hooks/useCategories.ts` 신규)
- `useCategories()` — 전체 조회
- `useCreateCategory()`, `useDeleteCategory()`

#### 4. 사이드바 (`Sidebar.tsx`)
- 카테고리 목록을 `useCategories()`로 서버에서 가져옴
- "카테고리 추가" 버튼 → 모달 열기 (`handleOpenCategoryModal` 연결)
- 카테고리 헤더 클릭 시 우클릭/버튼으로 수정/삭제

#### 5. CategoryModal 신규 (`components/folder/CategoryModal.tsx`)
- 카테고리명 입력 + 저장/취소

#### 6. FolderModal 수정 (`components/folder/FolderModal.tsx`)
- 카테고리 드롭다운: enum 하드코딩 → API에서 가져온 categories 목록 사용
- `categoryId` 로 submit

---

## 구현 순서

1. **백엔드**
   - V3 Flyway 마이그레이션 작성
   - Category 도메인/엔티티/레포지토리/서비스/컨트롤러 구현
   - Folder 관련 파일 수정 (FolderCategory enum → categoryId)
   - 빌드/테스트 확인

2. **프론트엔드** (백엔드 완료 후)
   - `types/folder.ts` 타입 수정
   - `services/categoryApi.ts` + `hooks/useCategories.ts` 추가
   - `CategoryModal.tsx` 신규 구현
   - `Sidebar.tsx` 카테고리 데이터 동적화 + 카테고리 추가 버튼 연결
   - `FolderModal.tsx` 드롭다운 동적화

---

## 수정 파일 목록

### 백엔드
| 파일 | 작업 |
|------|------|
| `db/migration/V3__category_table.sql` | 신규 |
| `folder/domain/model/Category.java` | 신규 |
| `folder/domain/model/FolderCategory.java` | 삭제 |
| `folder/adapter/out/persistence/CategoryJpaEntity.java` | 신규 |
| `folder/adapter/out/persistence/CategoryJpaRepository.java` | 신규 |
| `folder/adapter/in/web/CategoryController.java` | 신규 |
| `folder/adapter/in/web/CategoryResponse.java` | 신규 |
| `folder/adapter/in/web/CategoryCreateRequest.java` | 신규 |
| `folder/adapter/in/web/FolderJpaEntity.java` | 수정 |
| `folder/adapter/in/web/FolderCreateRequest.java` | 수정 |
| `folder/adapter/in/web/FolderUpdateRequest.java` | 수정 |
| `folder/adapter/in/web/FolderResponse.java` | 수정 |
| `folder/application/dto/FolderCreateCommand.java` | 수정 |
| `folder/application/dto/FolderUpdateCommand.java` | 수정 |
| `folder/application/dto/FolderResult.java` | 수정 |
| `folder/domain/model/Folder.java` | 수정 |
| `folder/adapter/out/persistence/FolderPersistenceAdapter.java` | 수정 |

### 프론트엔드
| 파일 | 작업 |
|------|------|
| `types/folder.ts` | 수정 |
| `services/categoryApi.ts` | 신규 |
| `hooks/useCategories.ts` | 신규 |
| `components/folder/CategoryModal.tsx` | 신규 |
| `components/folder/CategoryModal.module.css` | 신규 |
| `components/layout/Sidebar.tsx` | 수정 |
| `components/folder/FolderModal.tsx` | 수정 |

---

## 검증
1. `GET /api/categories` → 카테고리 목록 반환
2. `POST /api/categories` → 신규 카테고리 추가
3. 사이드바 "카테고리 추가" 버튼 → 모달 → 저장 → 사이드바 반영
4. "사업 추가" 모달 카테고리 드롭다운에 동적 목록 표시
5. 기존 폴더/사업 데이터 정상 조회

---

## Review 결과
- 검토일: 2026-04-17
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 확인 후 반영 (ADMIN 권한 적용, 카테고리 삭제 불가 정책 적용, categoryId 유효성 검증, Flyway 마이그레이션 안전 처리)

## 테스트 결과
- 백엔드: 70/70 통과
- 프론트엔드: 24/24 통과
