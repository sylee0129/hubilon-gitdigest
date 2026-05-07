import { create } from 'zustand'
import authApi from '../services/authApi'
import { queryClient } from '../queryClient'

interface User {
  id: number
  name: string
  email: string
  teamId: number | null
  teamName: string | null
  role: 'ADMIN' | 'USER'
}

interface AuthStore {
  user: User | null
  fetchUser: () => Promise<void>
  logout: () => void
  setUser: (user: User) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  fetchUser: async () => {
    const user = await authApi.me()
    set({ user })
  },
  logout: () => {
    queryClient.clear()
    set({ user: null })
    window.location.href = '/auth/logout'
  },
  setUser: (user) => set({ user }),
  clearAuth: () => set({ user: null }),
}))
