import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'

import Login from './pages/auth/Login'
import Registro from './pages/auth/Registro'
import Dashboard from './pages/Dashboard'
import ContribuyentesList from './pages/contribuyentes/ContribuyentesList'
import ContribuyenteLayout from './pages/contribuyentes/ContribuyenteLayout'
import Importaciones from './pages/contribuyentes/Importaciones'
import Conciliacion from './pages/contribuyentes/Conciliacion'
import Malla from './pages/contribuyentes/Malla'
import Criterios from './pages/contribuyentes/Criterios'
import Analisis from './pages/contribuyentes/Analisis'
import Matriz from './pages/contribuyentes/Matriz'
import ParametrosRiesgo from './pages/contribuyentes/ParametrosRiesgo'
import Resumen from './pages/contribuyentes/Resumen'
import Exportacion from './pages/contribuyentes/Exportacion'
import Respaldos from './pages/contribuyentes/Respaldos'
import Lista69b from './pages/Lista69b'
import EliminacionTotal from './pages/EliminacionTotal'
import Usuarios from './pages/admin/Usuarios'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/contribuyentes" element={<ContribuyentesList />} />
              <Route path="/contribuyentes/:id" element={<ContribuyenteLayout />}>
                <Route index element={<Navigate to="importaciones" replace />} />
                <Route path="importaciones" element={<Importaciones />} />
                <Route path="conciliacion" element={<Conciliacion />} />
                <Route path="malla" element={<Malla />} />
                <Route path="criterios" element={<Criterios />} />
                <Route path="analisis" element={<Analisis />} />
                <Route path="matriz" element={<Matriz />} />
                <Route path="parametros-riesgo" element={<ParametrosRiesgo />} />
                <Route path="resumen" element={<Resumen />} />
                <Route path="exportacion" element={<Exportacion />} />
                <Route path="respaldos" element={<Respaldos />} />
              </Route>
              <Route path="/lista-69b" element={<Lista69b />} />
              <Route path="/ajustes/eliminacion" element={<EliminacionTotal />} />

              <Route element={<AdminRoute />}>
                <Route path="/admin/usuarios" element={<Usuarios />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
