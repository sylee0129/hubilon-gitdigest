import type { Report } from '../../types/report'
import CommitList from './CommitList'
import styles from './ReportCard.module.css'

interface Props {
  report: Report
  showCheckbox?: boolean
  isSelected?: boolean
  onSelect?: (projectId: number, checked: boolean) => void
  isActive?: boolean
  onClick?: () => void
}

export default function ReportCard({ report, showCheckbox, isSelected, onSelect, isActive, onClick }: Props) {
  return (
    <div
      className={`${styles.card} ${isActive ? styles.activeCard : ''}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={(e) => e.key === 'Enter' && onClick?.()}
    >
      <div className={styles.cardHeader}>
        <div className={styles.projectLeft}>
          {showCheckbox && (
            <input
              type="checkbox"
              checked={isSelected ?? false}
              onChange={(e) => {
                e.stopPropagation()
                onSelect?.(report.projectId, e.target.checked)
              }}
              className={styles.checkbox}
              onClick={(e) => e.stopPropagation()}
            />
          )}
          <div className={styles.projectInfo}>
            <h3 className={styles.projectName}>{report.projectName}</h3>
            <span className={styles.dateRange}>{report.startDate} ~ {report.endDate}</span>
          </div>
        </div>
        <div className={styles.stats}>
          <span className={styles.statItem}>
            커밋 <strong>{report.commitCount}건</strong>
          </span>
          <span className={styles.statDot}>·</span>
          <span className={styles.statItem}>
            기여자 <strong>{report.contributorCount}명</strong>
          </span>
        </div>
      </div>

      <div className={styles.divider} />

      <CommitList commits={report.commits ?? []} />
    </div>
  )
}
