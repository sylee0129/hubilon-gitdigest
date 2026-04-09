import { useState } from 'react'
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
import { useReportStore } from '../../stores/useReportStore'
import AddProjectModal from '../project/AddProjectModal'
import type { Project } from '../../types/report'
import styles from './Sidebar.module.css'

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

interface Props {
  width?: number
}

export default function Sidebar({ width = 240 }: Props) {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const { data: projects, isLoading, isError } = useProjects()
  const deleteProject = useDeleteProject()
  const reorderProjects = useReorderProjects()
  const { selectedProjectId, activeTab, setSelectedProject, setTab } = useReportStore()

  const sensors = useSensors(
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
      </aside>

      {isModalOpen && (
        <AddProjectModal onClose={() => setIsModalOpen(false)} />
      )}
    </>
  )
}
