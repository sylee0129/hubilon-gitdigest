# 사이드바 폴더 카테고리 그룹핑 UI 구현 계획

## Context

사이드바 FOLDERS 목록을 폴더의 `category` 필드(DEVELOPMENT/NEW_BUSINESS/OTHER)를 기준으로 그룹화하여 표시.
현재는 `folders.map()`으로 단일 리스트 렌더링 중 → 그룹 헤더 + 아코디언 구조로 개편.

---

## 수정 대상 파일

- `frontend/src/components/layout/Sidebar.tsx`
- `frontend/src/components/layout/Sidebar.module.css`

---

## 구현 계획

### 1. 그룹화 로직

`folders` 배열을 카테고리별로 분리 (기존 `CATEGORY_LABELS` 재사용):

```ts
import { CATEGORY_LABELS } from '../../types/folder'

const CATEGORY_ORDER = ['DEVELOPMENT', 'NEW_BUSINESS', 'OTHER'] as const

const groupedFolders = CATEGORY_ORDER.map((cat) => ({
  category: cat,
  label: CATEGORY_LABELS[cat],
  folders: (folders ?? [])
    .filter((f) => f.category === cat)
    .sort((a, b) => a.name.localeCompare(b.name, 'ko')),
})).filter((g) => g.folders.length > 0)
```

### 2. 그룹 접기/펴기 상태

```ts
const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(new Set())

const toggleGroup = (cat: string) => {
  setCollapsedGroups((prev) => {
    const next = new Set(prev)
    next.has(cat) ? next.delete(cat) : next.add(cat)
    return next
  })
}
```

### 3. 렌더링 구조

그룹별 독립 DndContext (드래그는 그룹 내에서만):

```tsx
{groupedFolders.map(({ category, label, folders: groupFolders }) => {
  const isCollapsed = collapsedGroups.has(category)
  return (
    <div key={category} className={styles.categoryGroup}>
      <button className={styles.categoryHeader} onClick={() => toggleGroup(category)}>
        <span className={styles.categoryArrow}>{isCollapsed ? '▶' : '▼'}</span>
        <span className={styles.categoryLabel}>{label}</span>
        <span className={styles.categoryCount}>{groupFolders.length}</span>
      </button>

      {!isCollapsed && (
        <DndContext sensors={folderSensors} collisionDetection={closestCenter} onDragEnd={(e) => handleFolderDragEnd(e, category)}>
          <SortableContext items={groupFolders.map((f) => f.id)} strategy={verticalListSortingStrategy}>
            {groupFolders.map((folder) => (
              <SortableFolderItem key={folder.id} folder={folder} ... />
            ))}
          </SortableContext>
        </DndContext>
      )}
    </div>
  )
})}
```

`handleFolderDragEnd`에 `category` 파라미터 추가하여 그룹 내 정렬만 처리.

### 4. CSS 추가

```css
.categoryGroup { margin-bottom: 8px; }

.categoryHeader {
  display: flex; align-items: center; gap: 6px;
  width: 100%; padding: 4px 8px;
  background: none; border: none; cursor: pointer;
  color: #888; font-size: 11px; font-weight: 600;
  border-left: 2px solid #ddd;
}
.categoryHeader:hover { color: #555; border-left-color: #aaa; }
.categoryArrow { font-size: 9px; width: 10px; }
.categoryLabel { flex: 1; text-align: left; }
.categoryCount { font-size: 10px; color: #aaa; }
```

---

## 변경 요약

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 폴더 렌더링 | 단일 DndContext | 그룹별 DndContext |
| 정렬 | sortOrder | 그룹 고정순 → 그룹 내 가나다순 |
| 드래그 범위 | 전체 폴더 | 같은 카테고리 내 |
| 그룹 헤더 | 없음 | 아코디언 토글 |
| 빈 그룹 | - | 헤더 숨김 |

---

## 검증

1. `cd frontend && npm run dev`
2. 카테고리별 그룹 헤더 노출 확인
3. 헤더 클릭 시 아코디언 동작 확인
4. 폴더 없는 카테고리 헤더 미노출 확인
5. 기존 폴더 동작(클릭/확장/세부 프로젝트) 유지 확인

---

## Review 결과
- 검토일: 2026-04-10
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이상 없음 (구현 시 주의사항 반영)
  - 그룹 아코디언 상태명을 `collapsedCategories: Set<string>`으로 명확히 분리할 것
  - 드래그 완료 시 sortOrder는 그룹 내 상대 위치 기준으로 계산할 것
