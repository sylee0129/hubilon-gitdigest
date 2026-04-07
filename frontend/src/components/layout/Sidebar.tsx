import { useState } from 'react'
import { useProjects, useDeleteProject } from '../../hooks/useProjects'
import { useReportStore } from '../../stores/useReportStore'
import AddProjectModal from '../project/AddProjectModal'
import styles from './Sidebar.module.css'

interface Props {
  width?: number
}

export default function Sidebar({ width = 240 }: Props) {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const { data: projects, isLoading, isError } = useProjects()
  const deleteProject = useDeleteProject()
  const { selectedProjectId, activeTab, setSelectedProject, setTab } = useReportStore()

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

  return (
    <>
      <aside className={styles.sidebar} style={{ width: `${width}px`, minWidth: `${width}px` }}>
        <button className={styles.addBtn} onClick={() => setIsModalOpen(true)}>
          + 프로젝트 추가
        </button>

        <div className={styles.divider} />

        <nav className={styles.projectList}>
          {isLoading && (
            <div className={styles.stateMsg}>불러오는 중...</div>
          )}
          {isError && (
            <div className={styles.errorMsg}>프로젝트를 불러오지 못했습니다.</div>
          )}
          {projects?.map((project) => (
            <div
              key={project.id}
              className={`${styles.projectItem} ${
                selectedProjectId === project.id && activeTab === 'individual'
                  ? styles.active
                  : ''
              }`}
              onClick={() => handleProjectClick(project.id)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && handleProjectClick(project.id)}
            >
              <span className={styles.projectDot} />
              <span className={styles.projectName}>{project.name}</span>
              <button
                className={styles.deleteBtn}
                onClick={(e) => handleDelete(e, project.id)}
                title="프로젝트 삭제"
              >
                ✕
              </button>
            </div>
          ))}
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
