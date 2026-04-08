import { useState, useRef, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useReportStore } from '../../stores/useReportStore'
import styles from './Header.module.css'

interface WeekOption {
  label: string
  startDate: string
  endDate: string
}

function getMonday(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  d.setHours(0, 0, 0, 0)
  return d
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function generateWeeks(): WeekOption[] {
  const today = new Date()
  const thisMonday = getMonday(today)
  const threeMonthsAgo = new Date(today)
  threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
  const firstMonday = getMonday(threeMonthsAgo)

  const weeks: WeekOption[] = []
  const cursor = new Date(thisMonday)

  while (cursor >= firstMonday) {
    const monday = new Date(cursor)
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)

    const diffWeeks = Math.round((thisMonday.getTime() - monday.getTime()) / (7 * 24 * 60 * 60 * 1000))
    let label: string
    if (diffWeeks === 0) label = '이번 주'
    else if (diffWeeks === 1) label = '지난 주'
    else label = `${diffWeeks}주 전`

    weeks.push({
      label,
      startDate: formatDate(monday),
      endDate: formatDate(sunday),
    })

    cursor.setDate(cursor.getDate() - 7)
  }

  return weeks
}

export default function Header() {
  const { startDate, endDate, setCustomRange, setThisWeek, setPrevWeek, setNextWeek } = useReportStore()
  const queryClient = useQueryClient()
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const weeks = generateWeeks()

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <span className={styles.logo}>Hubilon GitDigest</span>
      </div>

      <div className={styles.center}>
        <button className={styles.navBtn} onClick={setPrevWeek}>‹</button>

        <div className={styles.datePickerWrapper} ref={dropdownRef}>
          <button
            className={styles.dateRangeBtn}
            onClick={() => setIsOpen(!isOpen)}
          >
            {startDate} ~ {endDate}
            <span className={styles.chevron}>▾</span>
          </button>

          {isOpen && (
            <div className={styles.dropdown}>
              {weeks.map((w) => (
                <button
                  key={w.startDate}
                  className={`${styles.weekOption} ${w.startDate === startDate ? styles.selectedWeek : ''}`}
                  onClick={() => {
                    setCustomRange(w.startDate, w.endDate)
                    setIsOpen(false)
                  }}
                >
                  <span className={styles.weekLabel}>{w.label}</span>
                  <span className={styles.weekDate}>{w.startDate} ~ {w.endDate}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        <button className={styles.navBtn} onClick={setNextWeek}>›</button>

        <button className={styles.thisWeekBtn} onClick={() => {
          setThisWeek()
          setIsOpen(false)
          void queryClient.invalidateQueries({ queryKey: ['reports'] })
        }}>
          이번주
        </button>
      </div>

      <div className={styles.right}>
        <span className={styles.userName}>사용자</span>
      </div>
    </header>
  )
}
