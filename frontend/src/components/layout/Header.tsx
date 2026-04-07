import { useReportStore } from '../../stores/useReportStore'
import styles from './Header.module.css'

export default function Header() {
  const { startDate, endDate, setPrevWeek, setNextWeek, setThisWeek } = useReportStore()

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <span className={styles.logo}>Hubilon GitDigest</span>
      </div>

      <div className={styles.center}>
        <button className={styles.navBtn} onClick={setPrevWeek} title="이전 주">
          &lt; 이전주
        </button>
        <span className={styles.dateRange}>
          {startDate} ~ {endDate}
        </span>
        <button className={styles.navBtn} onClick={setNextWeek} title="다음 주">
          다음주 &gt;
        </button>
        <button className={styles.thisWeekBtn} onClick={setThisWeek}>
          이번주
        </button>
      </div>

      <div className={styles.right}>
        <span className={styles.userName}>사용자</span>
      </div>
    </header>
  )
}
