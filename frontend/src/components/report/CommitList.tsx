import { useState } from 'react'
import type { CommitInfo } from '../../types/report'
import styles from './CommitList.module.css'

interface Props {
  commits: CommitInfo[]
}

const PAGE_SIZE = 20

function cleanMessage(message: string): string {
  return message
    .replace(/\s*\([^)]*(?:\.[a-zA-Z0-9]+|외 \d+개|\+\d+\/\-\d+)[^)]*\)\s*$/, '')
    .trim()
}

function groupByAuthor(commits: CommitInfo[]): [string, CommitInfo[]][] {
  const map = new Map<string, CommitInfo[]>()
  for (const commit of commits) {
    if (!map.has(commit.authorName)) map.set(commit.authorName, [])
    map.get(commit.authorName)!.push(commit)
  }
  return Array.from(map.entries())
}

function getFileLabel(file: { newFile: boolean; deletedFile: boolean; renamedFile: boolean; newPath: string; oldPath: string }) {
  if (file.newFile) return { icon: '+', cls: styles.fileNew, path: file.newPath }
  if (file.deletedFile) return { icon: '-', cls: styles.fileDeleted, path: file.oldPath }
  if (file.renamedFile) return { icon: '→', cls: styles.fileRenamed, path: `${file.oldPath} → ${file.newPath}` }
  return { icon: '~', cls: styles.fileModified, path: file.newPath }
}

export default function CommitList({ commits }: Props) {
  const [isOpen, setIsOpen] = useState(true)
  const [showAll, setShowAll] = useState(false)
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set())

  if (commits.length === 0) return null

  const displayed = showAll ? commits : commits.slice(0, PAGE_SIZE)
  const hasMore = commits.length > PAGE_SIZE
  const groups = groupByAuthor(displayed)

  const toggleCommit = (id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className={styles.container}>
      <button
        className={styles.toggleBtn}
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <span>커밋 이력</span>
        <span className={`${styles.arrow} ${isOpen ? styles.open : ''}`}>▼</span>
        <span className={styles.count}>({commits.length}건)</span>
      </button>

      {isOpen && (
        <div className={styles.list}>
          {groups.map(([author, authorCommits]) => (
            <div key={author} className={styles.group}>
              <div className={styles.groupHeader}>
                <span className={styles.groupAuthor}>{author}</span>
                <span className={styles.groupCount}>{authorCommits.length}건</span>
              </div>

              {authorCommits.map((commit) => {
                const isExpanded = expandedIds.has(commit.id)
                const hasFiles = commit.fileChanges && commit.fileChanges.length > 0
                return (
                  <div key={commit.id} className={styles.commitBlock}>
                    <div
                      className={`${styles.commitRow} ${isExpanded ? styles.commitRowExpanded : ''}`}
                      onClick={() => toggleCommit(commit.id)}
                    >
                      <div className={styles.commitMain}>
                        <span className={styles.date}>{commit.committedAt?.slice(0, 10)}</span>
                        <span className={`${styles.message} ${isExpanded ? styles.messageExpanded : ''}`}>
                          {cleanMessage(commit.message)}
                        </span>
                      </div>
                      {hasFiles && (
                        <span className={`${styles.fileArrow} ${isExpanded ? styles.open : ''}`}>▼</span>
                      )}
                    </div>

                    {isExpanded && hasFiles && (
                      <div className={styles.fileList}>
                        {commit.fileChanges.map((file, idx) => {
                          const { icon, cls, path } = getFileLabel(file)
                          return (
                            <div key={idx} className={styles.fileItem}>
                              <span className={cls}>{icon}</span>
                              <span className={styles.filePath}>{path}</span>
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          ))}

          {hasMore && !showAll && (
            <button className={styles.showMoreBtn} onClick={() => setShowAll(true)}>
              더보기 ({commits.length - PAGE_SIZE}건 더)
            </button>
          )}
          {showAll && hasMore && (
            <button className={styles.showMoreBtn} onClick={() => setShowAll(false)}>
              접기
            </button>
          )}
        </div>
      )}
    </div>
  )
}