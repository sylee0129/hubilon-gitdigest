import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ReportDashboard from './pages/ReportDashboard'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ReportDashboard />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
