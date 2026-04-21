import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ReportDashboard from './pages/ReportDashboard'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import SchedulerPage from './pages/SchedulerPage'
import ConfluenceAdminPage from './pages/admin/confluence/ConfluenceAdminPage'
import ProtectedRoute from './components/common/ProtectedRoute'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<ReportDashboard />} />
          <Route path="/scheduler" element={<SchedulerPage />} />
          <Route path="/admin/confluence" element={<ConfluenceAdminPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
