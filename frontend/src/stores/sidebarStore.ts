import { create } from 'zustand'

interface SidebarState {
  isCollapsed: boolean
  sidebarWidth: number
  toggleSidebar: () => void
  setSidebarWidth: (w: number) => void
}

export const useSidebarStore = create<SidebarState>((set) => ({
  isCollapsed: false,
  sidebarWidth: 260,
  toggleSidebar: () => set((state) => ({ isCollapsed: !state.isCollapsed })),
  setSidebarWidth: (w) => set({ sidebarWidth: w }),
}))
