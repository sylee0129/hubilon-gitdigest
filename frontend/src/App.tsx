import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ReportDashboard from './pages/ReportDashboard'
import LoginPage from './pages/LoginPage'
import ProtectedRoute from './components/common/ProtectedRoute'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<ReportDashboard />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
