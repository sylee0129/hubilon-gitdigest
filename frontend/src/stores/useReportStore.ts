import { create } from 'zustand'

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

function getWeekRange(monday: Date): { startDate: string; endDate: string } {
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return {
    startDate: formatDate(monday),
    endDate: formatDate(sunday),
  }
}

const thisMonday = getMonday(new Date())
const initialRange = getWeekRange(thisMonday)

interface ReportState {
  startDate: string
  endDate: string
  activeTab: 'all' | 'individual'
  selectedProjectId: number | null

  setPrevWeek: () => void
  setNextWeek: () => void
  setThisWeek: () => void
  setTab: (tab: 'all' | 'individual') => void
  setSelectedProject: (id: number | null) => void
}

export const useReportStore = create<ReportState>((set, get) => ({
  startDate: initialRange.startDate,
  endDate: initialRange.endDate,
  activeTab: 'all',
  selectedProjectId: null,

  setPrevWeek: () => {
    const monday = new Date(get().startDate)
    monday.setDate(monday.getDate() - 7)
    const range = getWeekRange(monday)
    set(range)
  },

  setNextWeek: () => {
    const monday = new Date(get().startDate)
    monday.setDate(monday.getDate() + 7)
    const range = getWeekRange(monday)
    set(range)
  },

  setThisWeek: () => {
    const range = getWeekRange(getMonday(new Date()))
    set(range)
  },

  setTab: (tab) => set({ activeTab: tab }),

  setSelectedProject: (id) => set({ selectedProjectId: id }),
}))
