import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  CloudUpload,
  ListChecks,
  Grid3x3,
  SlidersHorizontal,
  Search,
  LayoutGrid,
  Settings2,
  FileText,
  Download,
  Archive,
} from 'lucide-react'
import { obtenerContribuyente } from '../../api/contribuyentes'
import { apiErrorMessage } from '../../api/client'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import EjercicioSelect from '../../components/common/EjercicioSelect'
import { ContribuyenteContext } from '../../context/ContribuyenteContext'

const TABS = [
  { to: 'importaciones', label: 'Importaciones', Icon: CloudUpload },
  { to: 'conciliacion', label: 'Conciliación', Icon: ListChecks },
  { to: 'malla', label: 'Malla mensual', Icon: Grid3x3 },
  { to: 'criterios', label: 'Criterios', Icon: SlidersHorizontal },
  { to: 'analisis', label: 'Hallazgos', Icon: Search },
  { to: 'matriz', label: 'Matriz de riesgo', Icon: LayoutGrid },
  { to: 'parametros-riesgo', label: 'Ponderaciones', Icon: Settings2 },
  { to: 'resumen', label: 'Resumen ejecutivo', Icon: FileText },
  { to: 'exportacion', label: 'Exportar', Icon: Download },
  { to: 'respaldos', label: 'Respaldos', Icon: Archive },
]

export default function ContribuyenteLayout() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [contribuyente, setContribuyente] = useState(null)
  const [ejercicio, setEjercicio] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    obtenerContribuyente(id)
      .then((data) => {
        setContribuyente(data)
        setEjercicio(data.ejercicio)
      })
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [id])

  if (cargando) return <LoadingSpinner />
  if (error) {
    return (
      <>
        <ErrorAlert message={error} />
        <button className="btn btn-secondary" onClick={() => navigate('/contribuyentes')}>
          Volver a contribuyentes
        </button>
      </>
    )
  }
  if (!contribuyente) return null

  return (
    <ContribuyenteContext.Provider value={{ contribuyente, ejercicio, setEjercicio }}>
      <div className="mb-3">
        <button className="btn btn-link p-0 mb-2" onClick={() => navigate('/contribuyentes')}>
          <ArrowLeft size={15} /> Contribuyentes
        </button>
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
          <div>
            <h3 className="mb-0">{contribuyente.rfc}</h3>
            <div className="text-secondary">{contribuyente.razonSocial}</div>
          </div>
          <EjercicioSelect value={ejercicio} onChange={setEjercicio} />
        </div>
      </div>

      <ul className="nav nav-pills flex-nowrap overflow-auto mb-4 pb-1">
        {TABS.map((tab) => (
          <li className="nav-item" key={tab.to}>
            <NavLink to={tab.to} className={({ isActive }) => `nav-link text-nowrap ${isActive ? 'active' : 'text-secondary'}`}>
              <tab.Icon size={15} />
              {tab.label}
            </NavLink>
          </li>
        ))}
      </ul>

      <Outlet />
    </ContribuyenteContext.Provider>
  )
}
