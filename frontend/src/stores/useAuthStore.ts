import { create } from 'zustand'
import authApi from '../services/authApi'

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
  accessToken: string | null
  refreshToken: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  setAccessToken: (token: string) => void
  setUser: (user: User) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  login: async (email, password) => {
    const data = await authApi.login(email, password)
    set({ user: data.user, accessToken: data.accessToken, refreshToken: data.refreshToken })
  },
  logout: async () => {
    const { refreshToken } = get()
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    } finally {
      set({ user: null, accessToken: null, refreshToken: null })
    }
  },
  setAccessToken: (token) => set({ accessToken: token }),
  setUser: (user) => set({ user }),
  clearAuth: () => set({ user: null, accessToken: null, refreshToken: null }),
}))
