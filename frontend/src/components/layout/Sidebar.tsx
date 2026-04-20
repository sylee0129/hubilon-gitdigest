import { useState, useRef, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
  arrayMove,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useProjects, useDeleteProject, useReorderProjects } from '../../hooks/useProjects'
import { useFolders, useDeleteFolder, useReorderFolders } from '../../hooks/useFolders'
import { useCategories } from '../../hooks/useCategories'
import { useReportStore } from '../../stores/useReportStore'
import { useSidebarStore } from '../../stores/sidebarStore'
import { useAuthStore } from '../../stores/useAuthStore'
import AddProjectModal from '../project/AddProjectModal'
import AssignFolderModal from '../project/AssignFolderModal'
import FolderModal from '../folder/FolderModal'
import CategoryModal from '../folder/CategoryModal'
import DeleteConfirmModal from '../common/DeleteConfirmModal'
import type { Project } from '../../types/report'
import type { Folder, WorkProjectItem, Category } from '../../types/folder'
import type { FolderOrderItem } from '../../services/folderApi'
import styles from './Sidebar.module.css'

// ─── GitLab 프로젝트 아이템 ───────────────────────────────────────────────────

interface SortableProjectItemProps {
  project: Project
  isActive: boolean
  onProjectClick: (id: number) => void
  onDelete: (e: React.MouseEvent, id: number) => void
  onMoveToFolder: (id: number) => void
}

function SortableProjectItem({ project, isActive, onProjectClick, onDelete, onMoveToFolder }: SortableProjectItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: project.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    if (menuOpen) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`${styles.projectItem} ${isActive ? styles.active : ''}`}
      onClick={() => onProjectClick(project.id)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onProjectClick(project.id)}
    >
      <span
        className={styles.dragHandle}
        {...attributes}
        {...listeners}
        onClick={(e) => e.stopPropagation()}
      >
        ⠿
      </span>
      <span className={styles.projectName}>{project.name}</span>
      <div className={styles.menuWrapper} ref={menuRef} onClick={(e) => e.stopPropagation()}>
        <button
          className={styles.menuBtn}
          onClick={(e) => { e.stopPropagation(); setMenuOpen((prev) => !prev) }}
          title="메뉴"
        >
          ⋮
        </button>
        {menuOpen && (
          <div className={styles.contextMenu}>
            <button
              className={styles.contextMenuItem}
              onClick={() => { setMenuOpen(false); onMoveToFolder(project.id) }}
            >
              폴더로 이동
            </button>
            <button
              className={`${styles.contextMenuItem} ${styles.contextMenuItemDanger}`}
              onClick={(e) => { setMenuOpen(false); onDelete(e, project.id) }}
            >
              삭제
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

// ─── 폴더 내 assigned 프로젝트 아이템 ────────────────────────────────────────

interface AssignedProjectItemProps {
  project: Project
  onProjectClick: (id: number) => void
  onProjectDelete: (e: React.MouseEvent, id: number) => void
  onMoveToFolder: (id: number) => void
}

function AssignedProjectItem({ project, onProjectClick, onProjectDelete, onMoveToFolder }: AssignedProjectItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    if (menuOpen) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  return (
    <div className={styles.workProjectItem} onClick={() => onProjectClick(project.id)} role="button" tabIndex={0} onKeyDown={(e) => e.key === 'Enter' && onProjectClick(project.id)}>
      <span className={styles.workProjectName}>{project.name}</span>
      <div className={styles.menuWrapper} ref={menuRef} onClick={(e) => e.stopPropagation()}>
        <button
          className={styles.menuBtn}
          onClick={(e) => { e.stopPropagation(); setMenuOpen((prev) => !prev) }}
          title="메뉴"
        >
          ⋮
        </button>
        {menuOpen && (
          <div className={styles.contextMenu}>
            <button
              className={styles.contextMenuItem}
              onClick={() => { setMenuOpen(false); onMoveToFolder(project.id) }}
            >
              폴더 이동
            </button>
            <button
              className={`${styles.contextMenuItem} ${styles.contextMenuItemDanger}`}
              onClick={(e) => { setMenuOpen(false); onProjectDelete(e, project.id) }}
            >
              삭제
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

// ─── 폴더 아이템 ─────────────────────────────────────────────────────────────

interface SortableFolderItemProps {
  folder: Folder
  isExpanded: boolean
  isSelected: boolean
  onToggle: (id: number) => void
  onFolderSelect: (id: number) => void
  onEdit: (folder: Folder) => void
  onDelete: (id: number) => void
  selectedWorkProjectId: number | null
  onWorkProjectClick: (id: number) => void
  assignedProjects: Project[]
  onProjectClick: (id: number) => void
  onProjectDelete: (e: React.MouseEvent, id: number) => void
  onMoveToFolder: (id: number) => void
  isCompleted?: boolean
}

function SortableFolderItem({
  folder,
  isExpanded,
  isSelected,
  onToggle,
  onFolderSelect,
  onEdit,
  onDelete,
  selectedWorkProjectId,
  onWorkProjectClick,
  assignedProjects,
  onProjectClick,
  onProjectDelete,
  onMoveToFolder,
  isCompleted,
}: SortableFolderItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: folder.id,
    disabled: isCompleted ?? false,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    if (menuOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  return (
    <div ref={setNodeRef} style={style} className={styles.folderItem}>
      <div className={`${styles.folderHeader} ${isSelected ? styles.folderHeaderActive : ''}`}>
        <span
          className={styles.dragHandle}
          {...attributes}
          {...listeners}
        >
          ⠿
        </span>
        <button
          className={styles.folderToggle}
          onClick={() => { onToggle(folder.id); onFolderSelect(folder.id) }}
        >
          <span className={styles.folderArrow}>{isExpanded ? '▾' : '▸'}</span>
          <span className={styles.folderName}>{folder.name}</span>
        </button>
        <div className={styles.menuWrapper} ref={menuRef}>
          <button
            className={styles.menuBtn}
            onClick={(e) => { e.stopPropagation(); setMenuOpen((prev) => !prev) }}
            title="메뉴"
          >
            ⋮
          </button>
          {menuOpen && (
            <div className={styles.contextMenu}>
              <button
                className={styles.contextMenuItem}
                onClick={() => { setMenuOpen(false); onEdit(folder) }}
              >
                수정
              </button>
              <button
                className={`${styles.contextMenuItem} ${styles.contextMenuItemDanger}`}
                onClick={() => { setMenuOpen(false); onDelete(folder.id) }}
              >
                삭제
              </button>
            </div>
          )}
        </div>
      </div>

      {isExpanded && assignedProjects.length > 0 && (
        <div className={styles.workProjectList}>
          {assignedProjects.map((p) => (
            <AssignedProjectItem
              key={`proj-${p.id}`}
              project={p}
              onProjectClick={onProjectClick}
              onProjectDelete={onProjectDelete}
              onMoveToFolder={onMoveToFolder}
            />
          ))}
        </div>
      )}

      {isExpanded && folder.workProjects.length > 0 && (
        <div className={styles.workProjectList}>
          {folder.workProjects
            .slice()
            .sort((a, b) => a.sortOrder - b.sortOrder)
            .map((wp: WorkProjectItem) => (
              <button
                key={wp.id}
                className={`${styles.workProjectItem} ${selectedWorkProjectId === wp.id ? styles.workProjectActive : ''}`}
                onClick={() => onWorkProjectClick(wp.id)}
              >
                <span className={styles.workProjectName}>{wp.name}</span>
              </button>
            ))}
        </div>
      )}

      {isExpanded && folder.workProjects.length === 0 && assignedProjects.length === 0 && (
        <div className={styles.workProjectEmpty}>세부 프로젝트 없음</div>
      )}
    </div>
  )
}

// ─── Sidebar ─────────────────────────────────────────────────────────────────

interface Props {
  width?: number
}

export default function Sidebar({ width = 240 }: Props) {
  const navigate = useNavigate()
  const { isCollapsed, toggleSidebar } = useSidebarStore()
  const teamId = useAuthStore((s) => s.user?.teamId)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false)
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false)
  const [editingFolder, setEditingFolder] = useState<Folder | undefined>(undefined)
  const [editingCategory, setEditingCategory] = useState<Category | undefined>(undefined)
  const [expandedFolderIds, setExpandedFolderIds] = useState<Set<number>>(new Set())
  const [assigningProjectId, setAssigningProjectId] = useState<number | null>(null)
  const [collapsedCategories, setCollapsedCategories] = useState<Set<number>>(new Set())
  const [deletingProjectId, setDeletingProjectId] = useState<number | null>(null)
  const [isCompletedSectionOpen, setIsCompletedSectionOpen] = useState(false)

  const { data: projects, isLoading, isError } = useProjects()
  const { data: folders, isLoading: foldersLoading, isError: foldersError } = useFolders()
  const { data: categories } = useCategories()
  const deleteProject = useDeleteProject()
  const deleteFolder = useDeleteFolder()
  const reorderProjects = useReorderProjects()
  const reorderFolders = useReorderFolders()

  const {
    selectedProjectId,
    activeTab,
    selectedWorkProjectId,
    selectedFolderId,
    setSelectedProject,
    setTab,
    setSelectedWorkProject,
    setSelectedFolder,
  } = useReportStore()

  const completedFolders = useMemo(() =>
    (folders ?? [])
      .filter((f) => f.status === 'COMPLETED')
      .sort((a, b) => a.name.localeCompare(b.name, 'ko')),
    [folders]
  )

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } })
  )

  const folderSensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } })
  )

  const handleProjectClick = (id: number) => {
    setSelectedProject(id)
    setTab('individual')
    setSelectedFolder(null)
  }

  const handleFolderSelect = (id: number) => {
    setSelectedFolder(id)
  }

  const handleDelete = (e: React.MouseEvent, id: number) => {
    e.stopPropagation()
    setDeletingProjectId(id)
  }

  const handleConfirmDelete = () => {
    if (deletingProjectId === null) return
    deleteProject.mutate(deletingProjectId, {
      onSuccess: () => {
        if (selectedProjectId === deletingProjectId) {
          setSelectedProject(null)
        }
        setDeletingProjectId(null)
      },
    })
  }

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id || !projects) return

    const oldIndex = projects.findIndex((p) => p.id === active.id)
    const newIndex = projects.findIndex((p) => p.id === over.id)
    const reordered = arrayMove(projects, oldIndex, newIndex)
    reorderProjects.mutate(reordered.map((p) => p.id))
  }

  const handleFolderDragEnd = (event: DragEndEvent, categoryId: number) => {
    const { active, over } = event
    if (!over || active.id === over.id || !folders) return

    const categoryFolders = folders
      .filter((f) => f.categoryId === categoryId)
      .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
    const oldIndex = categoryFolders.findIndex((f) => f.id === active.id)
    const newIndex = categoryFolders.findIndex((f) => f.id === over.id)
    const reordered = arrayMove(categoryFolders, oldIndex, newIndex)
    const orders: FolderOrderItem[] = reordered.map((f, i) => ({ id: f.id, sortOrder: i + 1 }))
    reorderFolders.mutate(orders)
  }

  const toggleCategory = (catId: number) => {
    setCollapsedCategories((prev) => {
      const next = new Set(prev)
      next.has(catId) ? next.delete(catId) : next.add(catId)
      return next
    })
  }

  const handleToggleFolder = (id: number) => {
    setExpandedFolderIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const handleEditFolder = (folder: Folder) => {
    setEditingFolder(folder)
    setIsFolderModalOpen(true)
  }

  const handleDeleteFolder = (id: number) => {
    if (!confirm('폴더를 삭제하시겠습니까?')) return
    deleteFolder.mutate(
      { id },
      {
        onError: (err) => {
          const msg = err instanceof Error ? err.message : ''
          if (msg.includes('세부 프로젝트')) {
            if (confirm(`${msg}\n\n세부 프로젝트를 포함하여 강제 삭제하시겠습니까?`)) {
              deleteFolder.mutate({ id, force: true }, {
                onError: (e) => alert(e instanceof Error ? e.message : '삭제에 실패했습니다.'),
              })
            }
          } else {
            alert(msg || '삭제에 실패했습니다.')
          }
        },
      }
    )
  }

  const handleWorkProjectClick = (id: number) => {
    setSelectedWorkProject(id)
  }

  const handleOpenFolderModal = () => {
    setEditingFolder(undefined)
    setIsFolderModalOpen(true)
  }

  const handleCloseFolderModal = () => {
    setIsFolderModalOpen(false)
    setEditingFolder(undefined)
  }

  const handleOpenCategoryModal = () => {
    setEditingCategory(undefined)
    setIsCategoryModalOpen(true)
  }

  const handleEditCategory = (category: Category) => {
    setEditingCategory(category)
    setIsCategoryModalOpen(true)
  }

  const handleCloseCategoryModal = () => {
    setIsCategoryModalOpen(false)
    setEditingCategory(undefined)
  }

  const sidebarStyle = isCollapsed
    ? { width: '48px', minWidth: '48px' }
    : { width: `${width}px`, minWidth: `${Math.max(160, width)}px` }

  return (
    <>
      <aside className={`${styles.sidebar} ${isCollapsed ? styles.sidebarCollapsed : ''}`} style={sidebarStyle}>
        <div className={styles.toggleRow}>
          <button
            className={styles.toggleBtn}
            onClick={toggleSidebar}
            title={isCollapsed ? '사이드바 펼치기' : '사이드바 접기'}
          >
            {isCollapsed ? '→' : '←'}
          </button>
        </div>

        {!isCollapsed && (
          <>
        <nav className={styles.navList}>
          <button
            className={styles.navItem}
            onClick={() => { setSelectedFolder(null); setSelectedProject(null); navigate('/') }}
          >
            <svg className={styles.navIcon} viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="1" y="1" width="6" height="6" rx="1" fill="currentColor"/>
              <rect x="9" y="1" width="6" height="6" rx="1" fill="currentColor"/>
              <rect x="1" y="9" width="6" height="6" rx="1" fill="currentColor"/>
              <rect x="9" y="9" width="6" height="6" rx="1" fill="currentColor"/>
            </svg>
            대시보드
          </button>
          <button
            className={styles.navItem}
            onClick={() => navigate('/scheduler')}
          >
            <svg className={styles.navIcon} viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="1" y="3" width="14" height="12" rx="1.5" stroke="currentColor" strokeWidth="1.5"/>
              <path d="M5 1v4M11 1v4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              <path d="M1 7h14" stroke="currentColor" strokeWidth="1.5"/>
              <circle cx="5.5" cy="10.5" r="1" fill="currentColor"/>
              <circle cx="8" cy="10.5" r="1" fill="currentColor"/>
              <circle cx="10.5" cy="10.5" r="1" fill="currentColor"/>
            </svg>
            주간보고 스케줄러
          </button>
          {import.meta.env.VITE_CONFLUENCE_URL && (
            <a
              className={styles.navItem}
              href={import.meta.env.VITE_CONFLUENCE_URL as string}
              target="_blank"
              rel="noopener noreferrer"
            >
              <svg className={styles.navIcon} viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M2 3.5C2 2.67 2.67 2 3.5 2h9C13.33 2 14 2.67 14 3.5v9c0 .83-.67 1.5-1.5 1.5h-9C2.67 14 2 13.33 2 12.5v-9z" stroke="currentColor" strokeWidth="1.5"/>
                <path d="M5.5 10.5 8 5.5l2.5 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M6.3 9h3.4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
              Confluence
            </a>
          )}
        </nav>

        <div className={styles.divider} />

        <div className={styles.folderSectionHeader}>
          <div className={styles.sectionLabel}>Projects</div>
          <button className={styles.addFolderBtn} onClick={() => setIsModalOpen(true)}>
            + GitLab 프로젝트 추가
          </button>
        </div>

        <nav className={styles.projectList}>
          {teamId == null ? (
            <div className={styles.stateMsg}>팀에 배정되지 않았습니다.</div>
          ) : (
            <>
              {isLoading && (
                <div className={styles.stateMsg}>불러오는 중...</div>
              )}
              {isError && (
                <div className={styles.errorMsg}>프로젝트를 불러오지 못했습니다.</div>
              )}
              {projects && (
                <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                  {(() => {
                    const unassignedProjects = projects.filter((p) => !p.folderId)
                    return (
                      <SortableContext items={unassignedProjects.map((p) => p.id)} strategy={verticalListSortingStrategy}>
                        {unassignedProjects.map((project) => (
                          <SortableProjectItem
                            key={project.id}
                            project={project}
                            isActive={selectedProjectId === project.id && activeTab === 'individual'}
                            onProjectClick={handleProjectClick}
                            onDelete={handleDelete}
                            onMoveToFolder={setAssigningProjectId}
                          />
                        ))}
                      </SortableContext>
                    )
                  })()}
                </DndContext>
              )}
              {!isLoading && !isError && projects?.filter((p) => !p.folderId).length === 0 && (
                <div className={styles.stateMsg}>등록된 프로젝트가 없습니다.</div>
              )}
            </>
          )}
        </nav>

        {/* ─── Folders 섹션 ─────────────────────────────── */}
        <div className={styles.divider} />

        <div className={styles.folderSectionHeader}>
          <div className={styles.sectionLabel}>Folders</div>
          <div className={styles.addBtnGroup}>
            <button className={styles.addFolderBtn} onClick={handleOpenCategoryModal}>
              + 카테고리 추가
            </button>
            <button className={styles.addFolderBtn} onClick={handleOpenFolderModal}>
              + 프로젝트 추가
            </button>
          </div>
        </div>

        <div className={styles.folderSection}>
          {foldersLoading && (
            <div className={styles.stateMsg}>불러오는 중...</div>
          )}
          {foldersError && (
            <div className={styles.errorMsg}>폴더를 불러오지 못했습니다.</div>
          )}
          {folders && categories && (
            <>
              {categories
                .slice()
                .sort((a, b) => a.sortOrder - b.sortOrder)
                .map((cat) => {
                  const items = folders
                    .filter((f) => f.categoryId === cat.id && f.status === 'IN_PROGRESS')
                    .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
                  if (items.length === 0) return null
                  const isCollapsed = collapsedCategories.has(cat.id)
                  return (
                    <div key={cat.id} className={styles.categoryGroup}>
                      <button
                        className={styles.categoryHeader}
                        onClick={() => toggleCategory(cat.id)}
                      >
                        <span className={styles.categoryFolderIconWrap}>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" xmlns="http://www.w3.org/2000/svg" width="14" height="14">
                            <path d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"/>
                          </svg>
                        </span>
                        <span className={styles.categoryLabel}>{cat.name}</span>
                        <span className={styles.categoryCount}>({items.length})</span>
                      </button>
                      <button
                        className={styles.categoryEditBtn}
                        onClick={(e) => { e.stopPropagation(); handleEditCategory(cat) }}
                        title="카테고리 수정"
                      >
                        ✎
                      </button>
                      {!isCollapsed && (
                        <DndContext
                          sensors={folderSensors}
                          collisionDetection={closestCenter}
                          onDragEnd={(e) => handleFolderDragEnd(e, cat.id)}
                        >
                          <SortableContext items={items.map((f) => f.id)} strategy={verticalListSortingStrategy}>
                            {items.map((folder) => (
                              <SortableFolderItem
                                key={folder.id}
                                folder={folder}
                                isExpanded={expandedFolderIds.has(folder.id)}
                                isSelected={selectedFolderId === folder.id}
                                onToggle={handleToggleFolder}
                                onFolderSelect={handleFolderSelect}
                                onEdit={handleEditFolder}
                                onDelete={handleDeleteFolder}
                                selectedWorkProjectId={selectedWorkProjectId}
                                onWorkProjectClick={handleWorkProjectClick}
                                assignedProjects={projects?.filter((p) => p.folderId === folder.id) ?? []}
                                onProjectClick={handleProjectClick}
                                onProjectDelete={handleDelete}
                                onMoveToFolder={setAssigningProjectId}
                              />
                            ))}
                          </SortableContext>
                        </DndContext>
                      )}
                    </div>
                  )
                })}
            </>
          )}
          {!foldersLoading && !foldersError && folders?.length === 0 && (
            <div className={styles.stateMsg}>등록된 폴더가 없습니다.</div>
          )}

          {completedFolders.length > 0 && (
            <div className={styles.completedSection}>
              <button
                className={styles.completedSectionHeader}
                onClick={() => setIsCompletedSectionOpen((v) => !v)}
              >
                <span className={styles.completedSectionLine} />
                <span className={styles.completedSectionLabel}>
                  완료됨 ({completedFolders.length})
                </span>
                <span className={styles.completedSectionArrow}>
                  {isCompletedSectionOpen ? '▾' : '▸'}
                </span>
                <span className={styles.completedSectionLine} />
              </button>

              {isCompletedSectionOpen && (
                <div className={styles.completedFolderList}>
                  <SortableContext items={completedFolders.map((f) => f.id)} strategy={verticalListSortingStrategy}>
                    {completedFolders.map((folder) => (
                      <SortableFolderItem
                        key={folder.id}
                        folder={folder}
                        isCompleted
                        isExpanded={expandedFolderIds.has(folder.id)}
                        isSelected={selectedFolderId === folder.id}
                        onToggle={handleToggleFolder}
                        onFolderSelect={handleFolderSelect}
                        onEdit={handleEditFolder}
                        onDelete={handleDeleteFolder}
                        selectedWorkProjectId={selectedWorkProjectId}
                        onWorkProjectClick={handleWorkProjectClick}
                        assignedProjects={projects?.filter((p) => p.folderId === folder.id) ?? []}
                        onProjectClick={handleProjectClick}
                        onProjectDelete={handleDelete}
                        onMoveToFolder={setAssigningProjectId}
                      />
                    ))}
                  </SortableContext>
                </div>
              )}
            </div>
          )}
        </div>
          </>
        )}
      </aside>

      {isModalOpen && (
        <AddProjectModal onClose={() => setIsModalOpen(false)} />
      )}

      {isFolderModalOpen && (
        <FolderModal
          onClose={handleCloseFolderModal}
          folder={editingFolder}
        />
      )}

      {isCategoryModalOpen && (
        <CategoryModal
          isOpen={isCategoryModalOpen}
          onClose={handleCloseCategoryModal}
          initialCategory={editingCategory}
        />
      )}

      {assigningProjectId !== null && (
        <AssignFolderModal
          projectId={assigningProjectId}
          onClose={() => setAssigningProjectId(null)}
        />
      )}

      {deletingProjectId !== null && (
        <DeleteConfirmModal
          title="프로젝트 삭제"
          message={`'${projects?.find((p) => p.id === deletingProjectId)?.name ?? ''}' 프로젝트를 삭제하시겠습니까?\n연관된 보고서와 폴더 요약도 함께 삭제됩니다.`}
          isPending={deleteProject.isPending}
          onConfirm={handleConfirmDelete}
          onCancel={() => setDeletingProjectId(null)}
        />
      )}
    </>
  )
}
