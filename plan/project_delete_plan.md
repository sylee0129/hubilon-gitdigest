# 프로젝트 삭제 기능 개선 Plan

## Context
현재 프로젝트 삭제는 기능적으로 동작하지만 UX가 미완성 상태:
- `window.confirm()` 브라우저 기본 다이얼로그 사용 → 앱 디자인과 불일치
- 삭제 시 해당 프로젝트의 Report가 orphaned 상태로 남음 (projectId 있지만 Project 없음)
- 선택된 프로젝트 삭제 후 선택 상태 미초기화 가능성

## 구현 범위

### 1. Frontend: DeleteConfirmModal 컴포넌트 신규 생성
**파일**: `frontend/src/components/common/DeleteConfirmModal.tsx`

범용 삭제 확인 모달 (폴더 삭제에도 재사용 가능하도록):
```
Props:
  - title: string           // 모달 제목
  - message: string         // 확인 메시지
  - onConfirm: () => void   // 확인 콜백
  - onCancel: () => void    // 취소 콜백
  - isPending?: boolean     // 로딩 상태
```

스타일: 기존 `FolderModal` 패턴 참고 (`frontend/src/components/folder/FolderModal.tsx`)

### 2. Frontend: Sidebar.tsx 수정
**파일**: `frontend/src/components/layout/Sidebar.tsx`

- `handleDelete` 함수의 `window.confirm()` → `DeleteConfirmModal` 열기
- `deletingProjectId` state 추가 (모달 대상 관리)
- 삭제 후 해당 project가 선택 중이면 `setSelectedProject(null)` 처리
  - `useReportStore`의 `selectedProjectId` 확인

### 3. Backend: 삭제 시 연관 Report 처리
**파일**: `backend/src/main/java/com/hubilon/modules/project/application/service/command/ProjectDeleteService.java`

현재:
```java
void delete(Long id) {
  if (!projectQueryPort.existsById(id)) throw new NotFoundException(...)
  projectCommandPort.deleteById(id);
}
```

개선: Report 삭제 추가
- `ReportCommandPort.deleteByProjectId(Long)` 추가
- `ProjectDeleteService`에서 project 삭제 전 report 먼저 삭제

**정책: Option A 채택** — 프로젝트가 없으면 보고서도 의미 없으므로 함께 삭제

## 수정 파일 목록

### Frontend
| 파일 | 변경 내용 |
|------|---------|
| `frontend/src/components/common/DeleteConfirmModal.tsx` | **신규 생성** |
| `frontend/src/components/layout/Sidebar.tsx` | window.confirm → DeleteConfirmModal, 선택 상태 초기화 |

### Backend
| 파일 | 변경 내용 |
|------|---------|
| `backend/.../project/application/service/command/ProjectDeleteService.java` | Report + FolderSummary 연관 삭제 추가, `@Transactional` 단일 트랜잭션 |
| `backend/.../report/domain/port/out/ReportCommandPort.java` | `deleteByProjectId(Long)` 추가 |
| `backend/.../report/adapter/out/persistence/ReportPersistenceAdapter.java` | `deleteByProjectId` 구현 |
| `backend/.../report/adapter/out/persistence/ReportJpaRepository.java` | `deleteByProjectId` 쿼리 추가 |
| `backend/.../report/domain/port/out/FolderSummaryCommandPort.java` | `deleteByFolderId(Long)` 추가 |
| `backend/.../report/adapter/out/persistence/FolderSummaryPersistenceAdapter.java` | `deleteByFolderId` 구현 |
| `backend/.../report/adapter/out/persistence/FolderSummaryJpaRepository.java` | `deleteByFolderId` 쿼리 추가 |

### 삭제 순서 (ProjectDeleteService)
```
1. projectQueryPort.findById(id) → Project 조회 (folderId 확인용)
2. reportCommandPort.deleteByProjectId(id)
3. if (project.getFolderId() != null) folderSummaryCommandPort.deleteByFolderId(project.getFolderId())
4. projectCommandPort.deleteById(id)
```
모든 단계는 단일 `@Transactional` 내에서 실행.

## 검증
1. 프로젝트 삭제 버튼 클릭 → DeleteConfirmModal 노출 확인
2. 취소 클릭 → 모달 닫힘, 프로젝트 유지 확인
3. 확인 클릭 → 프로젝트 삭제 + 사이드바에서 제거 확인
4. 선택 중인 프로젝트 삭제 → 선택 상태 초기화 확인
5. DB 확인: `projects` 행 삭제, `reports` 연관 행 삭제, `folder_summary` 연관 행 삭제

## Review 결과
- 검토일: 2026-04-15
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: FolderSummary 삭제 범위 추가 반영 후 진행
