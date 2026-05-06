import Keycloak, { KeycloakTokenParsed } from 'keycloak-js'
import { create } from 'zustand'
import keycloak from '../lib/keycloak'

interface User {
  id: string
  name: string
  email: string
  role: 'ROLE_ADMIN' | 'ROLE_USER'
  departmentName: string | null
  teamName: string | null
  teamId: null
}

interface AuthStore {
  user: User | null
  getToken: () => string | undefined
  initFromKeycloak: (kc: Keycloak) => void
  logout: () => void
  // 하위 호환 — 기존 컴포넌트에서 accessToken 직접 참조하는 경우 대비
  accessToken: string | null
}

function parseDepartment(paths: string[] | undefined) {
  if (!paths?.length) return { departmentName: null, teamName: null }
  const parts = paths[0].split('/')
  return {
    departmentName: parts[2] ?? null,
    teamName: parts[3] ?? null,
  }
}

function resolveRole(tokenParsed: KeycloakTokenParsed | undefined): User['role'] {
  const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID
  const roles = tokenParsed?.resource_access?.[clientId]?.roles ?? []
  return roles.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER'
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  accessToken: null,
  getToken: () => keycloak.token,
  initFromKeycloak: (kc) => {
    const p = kc.tokenParsed
    const { departmentName, teamName } = parseDepartment(p?.department)
    const user: User = {
      id: p?.sub ?? '',
      name: p?.preferred_username ?? '',
      email: p?.email ?? '',
      role: resolveRole(p),
      departmentName,
      teamName,
      teamId: null,
    }
    set({
      user,
      accessToken: kc.token ?? null,
    })
  },
  logout: () => keycloak.logout({ redirectUri: window.location.origin }),
}))
