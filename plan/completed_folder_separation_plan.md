# 완료된 폴더 통합 분리 섹션 — 설계 플랜

## 목표

사이드바에서 **모든 카테고리(개발사업/신규추진사업/기타)의 완료(COMPLETED) 폴더**를 카테고리 구분 없이 하나의 통합 섹션으로 분리해 표시한다.

---

## 현재 구조 vs 목표 구조

### 현재
```
▼ 개발사업
  · KT 사장이지 쇼핑...  진행중
  · 공통 소스 개발        완료   ← 진행중과 섞임
▼ 기타
  · 넷코어 동아리...      완료   ← 다른 카테고리 완료도 각자 흩어짐
```

### 목표
```
▼ 개발사업              ← IN_PROGRESS 폴더만 표시
  · KT 사장이지 쇼핑...  진행중

(진행중 폴더 없는 카테고리는 헤더 자체 숨김)

── 완료됨 (3) ▸ ──     ← 기본 접힘, 모든 카테고리 완료 폴더 통합
  · 공통 소스 개발
  · 넷코어 동아리...
```

---

## UI 상세 명세

### 1. 카테고리 그룹 변경
- IN_PROGRESS 폴더만 표시
- 해당 카테고리에 IN_PROGRESS 폴더가 0개면 **카테고리 헤더 자체 숨김**

### 2. 완료됨 통합 섹션
- 위치: Folders 섹션 최하단
- 표시 조건: COMPLETED 폴더가 1개 이상일 때
- 기본 상태: **접힘 (collapsed)**
- 헤더 외관:
  ```
  ── 완료됨 (N) ▸ ──
  ```
  양옆 얇은 수평선 + 레이블 + 화살표

### 3. 완료 폴더 아이템
- 기존 `SortableFolderItem` 재사용 (`isCompleted` prop 추가)
- 카테고리 뱃지 표시: `[개발사업]`, `[기타]` 등 — `Folder.category` 필드 사용 (`CATEGORY_LABELS` 매핑)
- 시각 처리: `opacity: 0.7` (흐릿하게)
- 정렬: 상위에서 `useMemo`로 가나다순 정렬 후 내려보냄 (매 렌더 재연산 방지)
- **DnD 비활성화**: `useSortable({ id, disabled: isCompleted })` — `SortableContext` 유지하되 드래그 비활성

---

## 상태 관리

```ts
// Sidebar.tsx 신규 상태
const [isCompletedSectionOpen, setIsCompletedSectionOpen] = useState(false)
```

---

## 렌더링 로직 변경

### 카테고리 그룹 (`~528라인`)
```tsx
// 변경 전
const items = folders.filter((f) => f.category === cat).sort(...)

// 변경 후
const inProgressItems = folders
  .filter((f) => f.category === cat && f.status === 'IN_PROGRESS')
  .sort((a, b) => a.name.localeCompare(b.name, 'ko'))

if (inProgressItems.length === 0) return null  // 카테고리 전체 숨김
// inProgressItems만 SortableFolderItem으로 렌더
```

### 완료 통합 섹션 (folderSection div 하단에 추가)
```tsx
const completedFolders = (folders ?? [])
  .filter((f) => f.status === 'COMPLETED')
  .sort((a, b) => a.name.localeCompare(b.name, 'ko'))

{completedFolders.length > 0 && (
  <div className={styles.completedSection}>
    <button
      className={styles.completedSectionHeader}
      onClick={() => setIsCompletedSectionOpen((v) => !v)}
    >
      <span className={styles.completedSectionLine} />
      <span className={styles.completedSectionLabel}>완료됨 ({completedFolders.length})</span>
      <span>{isCompletedSectionOpen ? '▾' : '▸'}</span>
      <span className={styles.completedSectionLine} />
    </button>

    {isCompletedSectionOpen && (
      <div className={styles.completedFolderList}>
        {completedFolders.map((folder) => (
          <SortableFolderItem
            key={folder.id}
            folder={folder}
            isCompleted  // ← 카테고리 뱃지 + opacity 처리용
            ...기존 props
          />
        ))}
      </div>
    )}
  </div>
)}
```

### `SortableFolderItem` 변경
- `isCompleted?: boolean` prop 추가
- `isCompleted`이면 폴더명 앞에 카테고리 뱃지 렌더:
  ```tsx
  {isCompleted && (
    <span className={styles.categoryBadge}>{CATEGORY_LABELS[folder.category]}</span>
  )}
  ```

---

## CSS 추가 (`Sidebar.module.css`)

```css
.completedSection {
  margin-top: 12px;
}

.completedSectionHeader {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 4px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-tertiary);
  font-size: 11px;
  font-weight: 600;
}

.completedSectionHeader:hover { color: var(--color-text-secondary); }

.completedSectionLine {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.completedSectionLabel { white-space: nowrap; }

.completedFolderList {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-top: 4px;
  opacity: 0.7;
}

.categoryBadge {
  font-size: 9px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 8px;
  background-color: #f3f4f6;
  color: #6b7280;
  white-space: nowrap;
  flex-shrink: 0;
}
```

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `frontend/src/components/layout/Sidebar.tsx` | 카테고리 렌더링 필터, 완료 섹션 추가, SortableFolderItem prop 추가 |
| `frontend/src/components/layout/Sidebar.module.css` | 완료 섹션 + 카테고리 뱃지 스타일 |

---

## 구현 순서

1. `Sidebar.tsx` — `isCompletedSectionOpen` 상태 추가
2. `Sidebar.tsx` — 카테고리 그룹: IN_PROGRESS만 렌더, 0개면 return null
3. `Sidebar.tsx` — `SortableFolderItemProps`에 `isCompleted?: boolean` 추가
4. `Sidebar.tsx` — `SortableFolderItem` 내부 카테고리 뱃지 렌더
5. `Sidebar.tsx` — folderSection 하단 완료 통합 섹션 렌더링
6. `Sidebar.module.css` — 스타일 추가
7. 브라우저 확인

---

## Review 결과
- 검토일: 2026-04-16
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 5개 중 2개 비이슈(카테고리 필드 기존 존재, 폴더 추가 버튼 섹션 상단에 있음), 나머지 3개 플랜에 반영 (DnD disabled, useMemo 정렬, categoryBadge)

---

## 예외 처리

| 케이스 | 처리 |
|--------|------|
| COMPLETED 폴더 없음 | 완료 섹션 미표시 |
| 모든 폴더가 COMPLETED | 카테고리 헤더 전부 숨김, 완료 섹션만 표시 |
| 카테고리 자체가 접힌 상태 | 카테고리 토글은 IN_PROGRESS 전용이므로 영향 없음 |
