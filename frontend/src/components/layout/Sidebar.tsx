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
import FolderModal from '../folder/FolderModal'
import type { Project } from '../../types/report'
import type { Folder, WorkProjectItem } from '../../types/folder'
import { STATUS_LABELS } from '../../types/folder'
import type { FolderOrderItem } from '../../services/folderApi'
import styles from './Sidebar.module.css'

// ─── GitLab 프로젝트 아이템 ───────────────────────────────────────────────────

interface SortableProjectItemProps {
  project: Project
  isActive: boolean
  onProjectClick: (id: number) => void
  onDelete: (e: React.MouseEvent, id: number) => void
}

function SortableProjectItem({ project, isActive, onProjectClick, onDelete }: SortableProjectItemProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: project.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

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
      <button
        className={styles.deleteBtn}
        onClick={(e) => onDelete(e, project.id)}
        title="프로젝트 삭제"
      >
        ✕
      </button>
    </div>
  )
}

// ─── 폴더 아이템 ─────────────────────────────────────────────────────────────

interface SortableFolderItemProps {
  folder: Folder
  isExpanded: boolean
  onToggle: (id: number) => void
  onEdit: (folder: Folder) => void
  onDelete: (id: number) => void
  selectedWorkProjectId: number | null
  onWorkProjectClick: (id: number) => void
}

function SortableFolderItem({
  folder,
  isExpanded,
  onToggle,
  onEdit,
  onDelete,
  selectedWorkProjectId,
  onWorkProjectClick,
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
      <div className={styles.folderHeader}>
        <span
          className={styles.dragHandle}
          {...attributes}
          {...listeners}
        >
          ⠿
        </span>
        <button
          className={styles.folderToggle}
          onClick={() => onToggle(folder.id)}
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

      {isExpanded && folder.workProjects.length === 0 && (
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
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false)
  const [editingFolder, setEditingFolder] = useState<Folder | undefined>(undefined)
  const [expandedFolderIds, setExpandedFolderIds] = useState<Set<number>>(new Set())

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
    setSelectedProject,
    setTab,
    setSelectedWorkProject,
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

  const handleFolderDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id || !folders) return

    const oldIndex = folders.findIndex((f) => f.id === active.id)
    const newIndex = folders.findIndex((f) => f.id === over.id)
    const reordered = arrayMove(folders, oldIndex, newIndex)
    const orders: FolderOrderItem[] = reordered.map((f, i) => ({ id: f.id, sortOrder: i + 1 }))
    reorderFolders.mutate(orders)
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
              <SortableContext items={projects.map((p) => p.id)} strategy={verticalListSortingStrategy}>
                {projects.map((project) => (
                  <SortableProjectItem
                    key={project.id}
                    project={project}
                    isActive={selectedProjectId === project.id && activeTab === 'individual'}
                    onProjectClick={handleProjectClick}
                    onDelete={handleDelete}
                  />
                ))}
              </SortableContext>
            </DndContext>
          )}
          {!isLoading && !isError && projects?.length === 0 && (
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
            <DndContext sensors={folderSensors} collisionDetection={closestCenter} onDragEnd={handleFolderDragEnd}>
              <SortableContext items={folders.map((f) => f.id)} strategy={verticalListSortingStrategy}>
                {folders.map((folder) => (
                  <SortableFolderItem
                    key={folder.id}
                    folder={folder}
                    isExpanded={expandedFolderIds.has(folder.id)}
                    onToggle={handleToggleFolder}
                    onEdit={handleEditFolder}
                    onDelete={handleDeleteFolder}
                    selectedWorkProjectId={selectedWorkProjectId}
                    onWorkProjectClick={handleWorkProjectClick}
                  />
                ))}
              </SortableContext>
            </DndContext>
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
    </>
  )
}

