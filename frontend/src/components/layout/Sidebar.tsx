import { useState, useRef, useEffect } from 'react'
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
import { useReportStore } from '../../stores/useReportStore'
import AddProjectModal from '../project/AddProjectModal'
import AssignFolderModal from '../project/AssignFolderModal'
import FolderModal from '../folder/FolderModal'
import type { Project } from '../../types/report'
import type { Folder, WorkProjectItem } from '../../types/folder'
import { STATUS_LABELS, CATEGORY_LABELS } from '../../types/folder'
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
      <span className={styles.projectDot} />
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
}: SortableFolderItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: folder.id,
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
        <span className={`${styles.statusBadge} ${folder.status === 'IN_PROGRESS' ? styles.statusInProgress : styles.statusCompleted}`}>
          {STATUS_LABELS[folder.status]}
        </span>
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
            <button
              key={`proj-${p.id}`}
              className={styles.workProjectItem}
              onClick={() => onProjectClick(p.id)}
            >
              <span className={styles.projectDot} />
              <span className={styles.workProjectName}>{p.name}</span>
            </button>
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
                <span className={styles.workProjectDot} />
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

const CATEGORY_ORDER = ['DEVELOPMENT', 'NEW_BUSINESS', 'OTHER'] as const
type FolderCategory = typeof CATEGORY_ORDER[number]

interface Props {
  width?: number
}

export default function Sidebar({ width = 240 }: Props) {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false)
  const [editingFolder, setEditingFolder] = useState<Folder | undefined>(undefined)
  const [expandedFolderIds, setExpandedFolderIds] = useState<Set<number>>(new Set())
  const [assigningProjectId, setAssigningProjectId] = useState<number | null>(null)
  const [collapsedCategories, setCollapsedCategories] = useState<Set<FolderCategory>>(new Set())

  const { data: projects, isLoading, isError } = useProjects()
  const { data: folders, isLoading: foldersLoading, isError: foldersError } = useFolders()
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
    if (confirm('프로젝트를 삭제하시겠습니까?')) {
      deleteProject.mutate(id)
    }
  }

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id || !projects) return

    const oldIndex = projects.findIndex((p) => p.id === active.id)
    const newIndex = projects.findIndex((p) => p.id === over.id)
    const reordered = arrayMove(projects, oldIndex, newIndex)
    reorderProjects.mutate(reordered.map((p) => p.id))
  }

  const handleFolderDragEnd = (event: DragEndEvent, category: FolderCategory) => {
    const { active, over } = event
    if (!over || active.id === over.id || !folders) return

    const categoryFolders = folders
      .filter((f) => f.category === category)
      .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
    const oldIndex = categoryFolders.findIndex((f) => f.id === active.id)
    const newIndex = categoryFolders.findIndex((f) => f.id === over.id)
    const reordered = arrayMove(categoryFolders, oldIndex, newIndex)
    const orders: FolderOrderItem[] = reordered.map((f, i) => ({ id: f.id, sortOrder: i + 1 }))
    reorderFolders.mutate(orders)
  }

  const toggleCategory = (cat: FolderCategory) => {
    setCollapsedCategories((prev) => {
      const next = new Set(prev)
      next.has(cat) ? next.delete(cat) : next.add(cat)
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

  return (
    <>
      <aside className={styles.sidebar} style={{ width: `${width}px`, minWidth: `${width}px` }}>
        <button className={styles.addBtn} onClick={() => setIsModalOpen(true)}>
          + 프로젝트 추가
        </button>

        <div className={styles.divider} />

        <div className={styles.sectionLabel}>Projects</div>

        <nav className={styles.projectList}>
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
        </nav>

        {/* ─── Folders 섹션 ─────────────────────────────── */}
        <div className={styles.divider} />

        <div className={styles.folderSectionHeader}>
          <div className={styles.sectionLabel}>Folders</div>
          <button className={styles.addFolderBtn} onClick={handleOpenFolderModal}>
            + 폴더 추가
          </button>
        </div>

        <div className={styles.folderSection}>
          {foldersLoading && (
            <div className={styles.stateMsg}>불러오는 중...</div>
          )}
          {foldersError && (
            <div className={styles.errorMsg}>폴더를 불러오지 못했습니다.</div>
          )}
          {folders && (
            <>
              {CATEGORY_ORDER.map((cat) => {
                const label = CATEGORY_LABELS[cat]
                const STATUS_SORT: Record<string, number> = { IN_PROGRESS: 0, COMPLETED: 1 }
                const items = folders
                  .filter((f) => f.category === cat)
                  .sort((a, b) =>
                    STATUS_SORT[a.status] - STATUS_SORT[b.status] ||
                    a.name.localeCompare(b.name, 'ko')
                  )
                if (items.length === 0) return null
                const isCollapsed = collapsedCategories.has(cat)
                return (
                  <div key={cat} className={styles.categoryGroup}>
                    <button
                      className={styles.categoryHeader}
                      onClick={() => toggleCategory(cat)}
                    >
                      <span className={styles.categoryArrow}>{isCollapsed ? '▶' : '▼'}</span>
                      <span className={styles.categoryLabel}>{label}</span>
                      <span className={styles.categoryCount}>{items.length}</span>
                    </button>
                    {!isCollapsed && (
                      <DndContext
                        sensors={folderSensors}
                        collisionDetection={closestCenter}
                        onDragEnd={(e) => handleFolderDragEnd(e, cat)}
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
        </div>
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

      {assigningProjectId !== null && (
        <AssignFolderModal
          projectId={assigningProjectId}
          onClose={() => setAssigningProjectId(null)}
        />
      )}
    </>
  )
}

