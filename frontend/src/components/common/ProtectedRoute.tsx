import { Outlet } from 'react-router-dom'
import keycloak from '../../lib/keycloak'

export default function ProtectedRoute() {
  if (!keycloak.authenticated) {
    keycloak.login()
    return null
  }
  return <Outlet />
}
