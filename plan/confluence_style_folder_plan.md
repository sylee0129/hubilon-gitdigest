# Plan: 진행중인 폴더 섹션 Confluence 스타일 개편

## Context
현재 사이드바 폴더 섹션은 카테고리(개발사업/신규추진사업/기타) 헤더와 그 아래 폴더/프로젝트가 나열되는 구조.
사용자 요청: `[📁] 폴더명(건수)` 형태로 Confluence 스타일 트리 네비게이션으로 개편.

---

## 변경 범위

### 1. 카테고리 헤더 → Confluence 폴더 스타일

**현재:**
```
개발사업(1) ▼
```

**변경 후:**
```
📁 개발사업 (1) ▶
```

- 폴더 아이콘(📁) 왼쪽에 추가
- 건수는 `(n)` 형태 유지, 회색으로 표시
- 화살표 ▶(접힘) / ▼(펼침) 스타일
- 카테고리 헤더 Confluence 스타일 (패딩, 폰트, 호버)

### 2. 폴더 아이템 스타일

- 들여쓰기 증가 + 왼쪽 세로 indent 선
- 호버 시 행 하이라이트

### 3. 프로젝트 아이템 스타일

- 추가 들여쓰기로 계층 구조 명확화

---

## 수정 파일

### `frontend/src/components/layout/Sidebar.tsx`

카테고리 헤더 렌더링 부분:
```tsx
<button className={styles.categoryHeader} onClick={...}>
  <span className={styles.categoryArrow}>{isOpen ? '▼' : '▶'}</span>
  <span className={styles.categoryFolderIcon}>📁</span>
  <span className={styles.categoryLabel}>{CATEGORY_LABELS[category]}</span>
  <span className={styles.categoryCount}>({items.length})</span>
</button>
```

### `frontend/src/components/layout/Sidebar.module.css`

```css
/* 카테고리 헤더 */
.categoryHeader {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  background: none;
  border: none;
  cursor: pointer;
  width: 100%;
}
.categoryHeader:hover {
  background: var(--color-primary-light);
}

.categoryFolderIcon {
  font-size: 14px;
}

.categoryCount {
  color: var(--color-text-secondary);
  font-weight: normal;
  font-size: 12px;
  margin-left: 2px;
}

/* 폴더 아이템 들여쓰기 */
.folderHeader {
  padding-left: 20px;
  border-left: 2px solid var(--color-border-subtle);
  margin-left: 12px;
}

/* 프로젝트 아이템 들여쓰기 */
.workProjectItem {
  padding-left: 32px;
}
```

---

## 구현 순서

1. `Sidebar.tsx` — 카테고리 헤더에 폴더 아이콘 + 건수 포맷 변경
2. `Sidebar.module.css` — 카테고리 헤더, 폴더 아이템, 프로젝트 아이템 스타일 수정

---

## 검증

1. 브라우저에서 사이드바 폴더 섹션 확인
2. `📁 개발사업 (1)` 형태 표시 확인
3. 폴더 expand/collapse 동작 확인
4. 들여쓰기 계층 구조 확인
5. 완료됨 섹션 동일 스타일 적용 확인

---

## Review 결과
- 검토일: 2026-04-17
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이상 없음 (순수 UI 변경)
