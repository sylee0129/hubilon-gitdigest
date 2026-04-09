import type { Report } from '../../types/report'
import CommitList from './CommitList'
import styles from './ReportCard.module.css'

interface Props {
  report: Report
}

export default function ReportCard({ report }: Props) {
  return (
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        <div className={styles.projectLeft}>
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
