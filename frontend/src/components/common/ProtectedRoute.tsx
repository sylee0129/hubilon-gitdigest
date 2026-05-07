import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuthStore } from '../../stores/useAuthStore'

export default function ProtectedRoute() {
  const { user, fetchUser } = useAuthStore()
  const [loading, setLoading] = useState(!user)

  useEffect(() => {
    if (!user) {
      fetchUser()
        .catch(() => { window.location.href = '/auth/login' })
        .finally(() => setLoading(false))
    }
  }, [])

  if (loading) return null
  if (!user) return null
  return <Outlet />
}
