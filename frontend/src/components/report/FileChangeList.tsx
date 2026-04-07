import { useState } from 'react'
import type { FileChange } from '../../types/report'
import styles from './FileChangeList.module.css'

interface Props {
  files: FileChange[]
}

const PAGE_SIZE = 20

function getFileBadge(file: FileChange): string {
  if (file.newFile) return 'N'
  if (file.deletedFile) return 'D'
  if (file.renamedFile) return 'R'
  return 'M'
}

function getFilePath(file: FileChange): string {
  return file.newPath || file.oldPath || '(경로 없음)'
}

export default function FileChangeList({ files }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [showAll, setShowAll] = useState(false)

  const displayedFiles = showAll ? files : files.slice(0, PAGE_SIZE)
  const hasMore = files.length > PAGE_SIZE

  return (
    <div className={styles.container}>
      <button
        className={styles.toggleBtn}
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <span>변경 파일 목록</span>
        <span className={`${styles.arrow} ${isOpen ? styles.open : ''}`}>▼</span>
        <span className={styles.count}>({files.length}개)</span>
      </button>

      {isOpen && (
        <div className={styles.fileList}>
          {displayedFiles.map((file, idx) => (
            <div key={idx} className={styles.fileRow}>
              <span className={`${styles.badge} ${styles[`badge${getFileBadge(file)}`]}`}>
                {getFileBadge(file)}
              </span>
              <span className={styles.filePath}>{getFilePath(file)}</span>
              <span className={styles.additions}>+{file.addedLines}</span>
              <span className={styles.deletions}>-{file.removedLines}</span>
            </div>
          ))}

          {hasMore && !showAll && (
            <button
              className={styles.showMoreBtn}
              onClick={() => setShowAll(true)}
            >
              더보기 ({files.length - PAGE_SIZE}개 더)
            </button>
          )}

          {showAll && hasMore && (
            <button
              className={styles.showMoreBtn}
              onClick={() => setShowAll(false)}
            >
              접기
            </button>
          )}
        </div>
      )}
    </div>
  )
}
