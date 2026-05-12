import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ReportDashboard from './pages/ReportDashboard'
import SchedulerPage from './pages/SchedulerPage'
import ConfluenceAdminPage from './pages/admin/confluence/ConfluenceAdminPage'
import ProtectedRoute from './components/common/ProtectedRoute'
import { useAuthStore } from './stores/useAuthStore'


function AdminRoute({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  if (!user) return <Navigate to="/auth/login" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<ReportDashboard />} />
          <Route path="/scheduler" element={<SchedulerPage />} />
          <Route path="/admin/confluence" element={
            <AdminRoute><ConfluenceAdminPage /></AdminRoute>
          } />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
