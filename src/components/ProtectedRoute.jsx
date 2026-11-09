import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import LoadingSpinner from './common/LoadingSpinner'

export function ProtectedRoute() {
  const { isAuthenticated, cargando } = useAuth()
  const location = useLocation()

  if (cargando) return <LoadingSpinner label="Verificando sesión..." />
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />
  return <Outlet />
}

export function AdminRoute() {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <Navigate to="/" replace />
  return <Outlet />
}
