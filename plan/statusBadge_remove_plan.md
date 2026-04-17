# statusBadge 제거 + 진행중 섹션 헤더 스타일 변경

## 요구사항
- 각 폴더 아이템의 `진행중`/`완료` 뱃지(statusBadge) 제거
- 카테고리 헤더(개발사업, 신규추진사업 등)를 `완료됨 (N)` 섹션 헤더처럼 변경
  - 양쪽 가로선 + 레이블 + 화살표 구조

---

## 수정 파일

### 1. `frontend/src/components/layout/Sidebar.tsx`

#### 변경 1: statusBadge 제거 (line 247-249)
```tsx
// 삭제 대상
<span className={`${styles.statusBadge} ${folder.status === 'IN_PROGRESS' ? styles.statusInProgress : styles.statusCompleted}`}>
  {STATUS_LABELS[folder.status]}
</span>
```

#### 변경 2: categoryHeader 버튼 내부 구조 변경 (lines 548-555)
```tsx
// Before
<button className={styles.categoryHeader} onClick={() => toggleCategory(cat)}>
  <span className={styles.categoryArrow}>{isCollapsed ? '▶' : '▼'}</span>
  <span className={styles.categoryLabel}>{label}</span>
  <span className={styles.categoryCount}>{items.length}</span>
</button>

// After — completedSectionHeader 구조와 동일
<button className={styles.categoryHeader} onClick={() => toggleCategory(cat)}>
  <span className={styles.completedSectionLine} />
  <span className={styles.categoryLabel}>{label} ({items.length})</span>
  <span className={styles.categoryArrow}>{isCollapsed ? '▸' : '▾'}</span>
  <span className={styles.completedSectionLine} />
</button>
```

---

### 2. `frontend/src/components/layout/Sidebar.module.css`

#### `.categoryHeader` — border-left 제거, completedSectionHeader 스타일로
```css
/* Before */
.categoryHeader {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 4px 8px;
  background: none;
  border: none;
  border-left: 2px solid #ddd;
  cursor: pointer;
  color: #888;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

/* After */
.categoryHeader {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 4px;
  background: none;
  border: none;
  cursor: pointer;
  color: #aaa;
  font-size: 11px;
  font-weight: 500;
}
```

#### `.categoryHeader:hover`
```css
/* Before */
.categoryHeader:hover {
  color: #555;
  border-left-color: #aaa;
}

/* After */
.categoryHeader:hover {
  color: var(--color-text-secondary);
}
```

#### `.categoryLabel`
```css
/* Before */
.categoryLabel {
  flex: 1;
  text-align: left;
}

/* After */
.categoryLabel {
  white-space: nowrap;
}
```

#### `.categoryArrow`
```css
/* Before */
.categoryArrow {
  font-size: 9px;
  width: 10px;
  flex-shrink: 0;
}

/* After */
.categoryArrow {
  font-size: 10px;
  flex-shrink: 0;
}
```

#### `.categoryCount` — 제거 (레이블에 통합됨)

---

## 검증
1. `npm run dev` 후 사이드바 확인
2. 카테고리 헤더가 `완료됨 (N)` 처럼 양쪽 가로선으로 표시되는지 확인
3. 개별 폴더 뱃지 제거 확인
4. 카테고리 접기/펼치기 동작 정상 확인

---

## Review 결과
- 검토일: 2026-04-16
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 2건 발견 → 사용자 승인 후 구현 시 함께 정리
  - statusBadge 제거 후 statusInProgress / statusCompleted CSS 클래스 및 STATUS_LABELS 상수 정리 (다른 사용처 없으면 제거)
  - categoryArrow, categoryLabel, categoryCount dead CSS 클래스 정리
