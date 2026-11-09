import { useEffect, useState } from 'react'
import { obtenerConciliacion } from '../../api/conciliacion'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import { formatCurrency, formatDate } from '../../utils/format'

const LISTAS = [
  { key: 'cancelados', label: 'Cancelados' },
  { key: 'faltantes', label: 'Faltantes' },
  { key: 'sinMetadata', label: 'Sin metadata' },
  { key: 'canceladosFueraEjercicio', label: 'Cancel. fuera de ejercicio' },
  { key: 'canceladosFueraPlazo', label: 'Cancel. fuera de plazo' },
  { key: 'canceladosConPagoOEgreso', label: 'Cancel. con pago/egreso' },
]

function Stat({ label, value, danger }) {
  return (
    <div className="col-6 col-md-3 col-lg-2">
      <div className="border rounded p-2 h-100 bg-white">
        <div className="text-secondary small">{label}</div>
        <div className={`fw-semibold ${danger ? 'text-danger' : ''}`}>{value}</div>
      </div>
    </div>
  )
}

export default function Conciliacion() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [data, setData] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [tabActiva, setTabActiva] = useState('cancelados')

  useEffect(() => {
    if (!ejercicio) return
    setCargando(true)
    obtenerConciliacion(contribuyente.id, ejercicio)
      .then(setData)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [contribuyente.id, ejercicio])

  if (cargando) return <LoadingSpinner />
  if (error) return <ErrorAlert message={error} />
  if (!data) return null

  const t = data.tablero

  return (
    <div>
      <div className="card mb-4">
        <div className="card-body">
          <h6 className="card-title mb-3">Tablero de conciliación {t.ejercicio}</h6>
          <div className="row g-2 mb-3">
            <Stat label="XML cargados" value={t.xmlCargados} />
            <Stat label="Registros metadata" value={t.registrosMetadata} />
            <Stat label="Conciliados" value={t.conciliados} />
            <Stat label="Cancelados" value={t.cancelados} danger />
            <Stat label="Importe cancelado" value={formatCurrency(t.importeCancelado)} danger />
            <Stat label="Faltantes" value={t.faltantes} danger />
            <Stat label="Importe faltantes" value={formatCurrency(t.importeFaltantes)} danger />
            <Stat label="Sin metadata" value={t.sinMetadata} />
            <Stat label="Diferencias de monto" value={t.diferenciasMonto} danger />
            <Stat label="Cancel. fuera ejercicio" value={t.canceladosFueraEjercicio} />
            <Stat label="Cancel. fuera plazo" value={t.canceladosFueraPlazo} />
            <Stat label="Cancel. c/pago o egreso" value={t.canceladosConPagoOEgreso} />
          </div>

          <div className="row g-3">
            <GrupoTabla titulo="Cancelados por mes" grupos={t.canceladosPorMes} />
            <GrupoTabla titulo="Cancelados por tipo" grupos={t.canceladosPorTipo} />
            <GrupoTabla titulo="Cancelados por contraparte" grupos={t.canceladosPorContraparte} />
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header bg-white">
          <ul className="nav nav-tabs card-header-tabs flex-nowrap overflow-auto">
            {LISTAS.map((l) => (
              <li className="nav-item" key={l.key}>
                <button
                  className={`nav-link text-nowrap ${tabActiva === l.key ? 'active' : ''}`}
                  onClick={() => setTabActiva(l.key)}
                >
                  {l.label} <span className="badge rounded-pill bg-secondary-subtle text-secondary-emphasis ms-1">{(data[l.key] || []).length}</span>
                </button>
              </li>
            ))}
            <li className="nav-item">
              <button
                className={`nav-link text-nowrap ${tabActiva === 'diferencias' ? 'active' : ''}`}
                onClick={() => setTabActiva('diferencias')}
              >
                Diferencias de monto <span className="badge rounded-pill bg-secondary-subtle text-secondary-emphasis ms-1">{(data.diferencias || []).length}</span>
              </button>
            </li>
          </ul>
        </div>
        <div className="card-body">
          {tabActiva === 'diferencias' ? <TablaDiferencias filas={data.diferencias} /> : <TablaFilas filas={data[tabActiva]} />}
        </div>
      </div>
    </div>
  )
}

function GrupoTabla({ titulo, grupos }) {
  if (!grupos || grupos.length === 0) return null
  return (
    <div className="col-md-4">
      <div className="border rounded p-2 h-100">
        <div className="fw-semibold small mb-2">{titulo}</div>
        <table className="table table-sm mb-0">
          <tbody>
            {grupos.map((g) => (
              <tr key={g.clave}>
                <td className="small">{g.clave}</td>
                <td className="text-end small">{g.cantidad}</td>
                <td className="text-end small">{formatCurrency(g.importe)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function TablaFilas({ filas }) {
  if (!filas || filas.length === 0) return <div className="text-center text-secondary py-4">Sin registros.</div>
  return (
    <div className="table-responsive">
      <table className="table table-hover table-sm align-middle mb-0">
        <thead>
          <tr>
            <th>UUID</th>
            <th>Emisor</th>
            <th>Receptor</th>
            <th>Fecha</th>
            <th className="text-end">Monto</th>
            <th>Efecto</th>
            <th>Nota</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={f.uuid}>
              <td className="small font-monospace">{f.uuid}</td>
              <td className="small">
                {f.rfcEmisor}
                <br />
                <span className="text-secondary">{f.nombreEmisor}</span>
              </td>
              <td className="small">
                {f.rfcReceptor}
                <br />
                <span className="text-secondary">{f.nombreReceptor}</span>
              </td>
              <td className="small">{formatDate(f.fechaEmision)}</td>
              <td className="text-end small">{formatCurrency(f.monto)}</td>
              <td className="small">{f.efecto}</td>
              <td className="small text-secondary">{f.nota}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function TablaDiferencias({ filas }) {
  if (!filas || filas.length === 0) return <div className="text-center text-secondary py-4">Sin diferencias.</div>
  return (
    <div className="table-responsive">
      <table className="table table-hover table-sm align-middle mb-0">
        <thead>
          <tr>
            <th>UUID</th>
            <th>Contraparte</th>
            <th className="text-end">Monto XML</th>
            <th className="text-end">Monto metadata</th>
            <th className="text-end">Diferencia</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={f.uuid}>
              <td className="small font-monospace">{f.uuid}</td>
              <td className="small">{f.rfcContraparte}</td>
              <td className="text-end small">{formatCurrency(f.montoXml)}</td>
              <td className="text-end small">{formatCurrency(f.montoMetadata)}</td>
              <td className="text-end small text-danger">{formatCurrency(f.diferencia)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
