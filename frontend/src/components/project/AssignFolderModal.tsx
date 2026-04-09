import { useFolders } from '../../hooks/useFolders'
import { useMoveProjectToFolder } from '../../hooks/useProjects'
import styles from '../folder/FolderModal.module.css'

interface Props {
  projectId: number
  onClose: () => void
}

export default function AssignFolderModal({ projectId, onClose }: Props) {
  const { data: folders } = useFolders()
  const moveToFolder = useMoveProjectToFolder()

  const handleSelect = (folderId: number | null) => {
    moveToFolder.mutate({ id: projectId, folderId }, { onSuccess: onClose })
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2 className={styles.title}>폴더로 이동</h2>
          <button className={styles.closeBtn} onClick={onClose}>✕</button>
        </div>
        <div style={{ padding: '8px 0' }}>
          <button
            style={{ display: 'block', width: '100%', padding: '10px 20px', background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer', fontSize: '13px', color: 'var(--color-text-secondary)' }}
            onClick={() => handleSelect(null)}
          >
            미분류 (폴더 해제)
          </button>
          {folders?.map((folder) => (
            <button
              key={folder.id}
              style={{ display: 'block', width: '100%', padding: '10px 20px', background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer', fontSize: '13px', color: 'var(--color-text-primary)' }}
              onClick={() => handleSelect(folder.id)}
            >
              {folder.name}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
