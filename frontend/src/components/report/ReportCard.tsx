import type { Report } from '../../types/report'
import CommitList from './CommitList'
import { useGenerateAiSummary } from '../../hooks/useReports'
import styles from './ReportCard.module.css'

interface Props {
  report: Report
}

export default function ReportCard({ report }: Props) {
  const generateAiSummary = useGenerateAiSummary()

  return (
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        <div className={styles.projectInfo}>
          <h3 className={styles.projectName}>{report.projectName}</h3>
          <span className={styles.dateRange}>
            {report.startDate} ~ {report.endDate}
          </span>
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

      <div className={styles.summarySection}>
        <div className={styles.summaryHeader}>
          <button
            className={styles.aiBtn}
            onClick={() => generateAiSummary.mutate(report.id)}
            disabled={generateAiSummary.isPending}
          >
            {generateAiSummary.isPending ? '생성 중...' : '✨ AI 요약 생성'}
          </button>
        </div>
      </div>

      <div className={styles.divider} />

      <CommitList commits={report.commits ?? []} />
    </div>
  )
}
