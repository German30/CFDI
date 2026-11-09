import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Inbox, ArrowRight } from 'lucide-react'
import { obtenerConsolidado } from '../api/consolidado'
import { apiErrorMessage } from '../api/client'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorAlert from '../components/common/ErrorAlert'
import EjercicioSelect from '../components/common/EjercicioSelect'
import { formatCurrency, formatPercent } from '../utils/format'

export default function Dashboard() {
  const [ejercicio, setEjercicio] = useState(new Date().getFullYear())
  const [filas, setFilas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setCargando(true)
    setError(null)
    obtenerConsolidado(ejercicio)
      .then(setFilas)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [ejercicio])

  const totalHallazgosCriticos = filas.reduce((acc, f) => acc + (f.hallazgos?.CRITICO || 0), 0)
  const totalHallazgosAltos = filas.reduce((acc, f) => acc + (f.hallazgos?.ALTO || 0), 0)
  const totalOperaciones = filas.reduce((acc, f) => acc + (f.operaciones || 0), 0)

  return (
    <div>
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
        <h3 className="mb-0">Panel general</h3>
        <EjercicioSelect value={ejercicio} onChange={setEjercicio} />
      </div>

      <ErrorAlert message={error} onClose={() => setError(null)} />

      {cargando ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="card mb-4">
            <div className="d-flex flex-wrap">
              <div className="flex-fill p-3 px-md-4">
                <div className="text-secondary small">Contribuyentes</div>
                <div className="fs-3 fw-semibold">{filas.length}</div>
              </div>
              <div className="flex-fill p-3 px-md-4 border-start">
                <div className="text-secondary small">Operaciones totales</div>
                <div className="fs-3 fw-semibold">{totalOperaciones.toLocaleString('es-MX')}</div>
              </div>
              <div className="flex-fill p-3 px-md-4 border-start">
                <div className="text-secondary small">Hallazgos críticos</div>
                <div className="fs-3 fw-semibold text-danger-emphasis">{totalHallazgosCriticos}</div>
              </div>
              <div className="flex-fill p-3 px-md-4 border-start">
                <div className="text-secondary small">Hallazgos altos</div>
                <div className="fs-3 fw-semibold text-orange-emphasis">{totalHallazgosAltos}</div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-body">
              <h6 className="card-title mb-3">Comparativo por contribuyente</h6>
              {filas.length === 0 ? (
                <div className="text-center text-secondary py-5">
                  <Inbox size={36} className="mb-2" />
                  No tienes contribuyentes registrados para el ejercicio {ejercicio}.
                  <div className="mt-3">
                    <Link to="/contribuyentes" className="btn btn-primary btn-sm">
                      Agregar contribuyente
                    </Link>
                  </div>
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle">
                    <thead>
                      <tr>
                        <th>RFC</th>
                        <th>Razón social</th>
                        <th className="text-end">Ingresos emitidos</th>
                        <th className="text-end">Ingresos recibidos</th>
                        <th className="text-end">Egresos</th>
                        <th className="text-end">Nómina</th>
                        <th className="text-end">% cancelación</th>
                        <th className="text-end">Hallazgos</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {filas.map((f) => (
                        <tr key={f.id}>
                          <td className="fw-semibold">{f.rfc}</td>
                          <td>{f.razonSocial}</td>
                          <td className="text-end">{formatCurrency(f.ingresosEmitidos)}</td>
                          <td className="text-end">{formatCurrency(f.ingresosRecibidos)}</td>
                          <td className="text-end">{formatCurrency(f.egresosEmitidos)}</td>
                          <td className="text-end">{formatCurrency(f.nomina)}</td>
                          <td className="text-end">{formatPercent(f.pctCancelacion)}</td>
                          <td className="text-end">
                            {f.hallazgos?.CRITICO > 0 && (
                              <span className="badge rounded-pill bg-danger-subtle text-danger-emphasis ms-1">
                                {f.hallazgos.CRITICO} crítico
                              </span>
                            )}
                            {f.hallazgos?.ALTO > 0 && (
                              <span className="badge rounded-pill bg-orange-subtle text-orange-emphasis ms-1">
                                {f.hallazgos.ALTO} alto
                              </span>
                            )}
                            {f.hallazgos?.MODERADO > 0 && (
                              <span className="badge rounded-pill bg-warning-subtle text-warning-emphasis ms-1">
                                {f.hallazgos.MODERADO} mod.
                              </span>
                            )}
                            {f.hallazgos?.BAJO > 0 && (
                              <span className="badge rounded-pill bg-success-subtle text-success-emphasis ms-1">
                                {f.hallazgos.BAJO} bajo
                              </span>
                            )}
                          </td>
                          <td>
                            <Link to={`/contribuyentes/${f.id}/analisis`} className="btn btn-sm btn-outline-primary">
                              Ver <ArrowRight size={14} />
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
