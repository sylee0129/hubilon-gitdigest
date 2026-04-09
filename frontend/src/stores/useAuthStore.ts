import { create } from 'zustand'
import authApi from '../services/authApi'

interface User {
  id: number
  name: string
  email: string
  department: string | null
  role: 'ADMIN' | 'USER'
}

interface AuthStore {
  user: User | null
  accessToken: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  setAccessToken: (token: string) => void
  setUser: (user: User) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  accessToken: null,
  login: async (email, password) => {
    const data = await authApi.login(email, password)
    set({ user: data.user, accessToken: data.accessToken })
  },
  logout: async () => {
    try {
      await authApi.logout()
    } finally {
      set({ user: null, accessToken: null })
    }
  },
  setAccessToken: (token) => set({ accessToken: token }),
  setUser: (user) => set({ user }),
  clearAuth: () => set({ user: null, accessToken: null }),
}))
