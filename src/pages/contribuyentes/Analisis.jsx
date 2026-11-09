import { useEffect, useState } from 'react'
import { Play, ChevronRight } from 'lucide-react'
import {
  actualizarHallazgo,
  ejecutarAnalisis,
  listarHallazgos,
  obtenerHallazgo,
} from '../../api/analisis'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import Modal from '../../components/common/Modal'
import { NivelBadge, EstatusBadge } from '../../components/common/badges'
import { formatCurrency } from '../../utils/format'

const NIVELES = ['BAJO', 'MODERADO', 'ALTO', 'CRITICO']
const ESTATUSES = ['PENDIENTE', 'EN_REVISION', 'ACLARADO', 'CORREGIDO']

export default function Analisis() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [hallazgos, setHallazgos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [ejecutando, setEjecutando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [filtros, setFiltros] = useState({ nivel: '', estatus: '', criterio: '' })
  const [hallazgoSeleccionado, setHallazgoSeleccionado] = useState(null)

  function cargarHallazgos() {
    if (!ejercicio) return
    setCargando(true)
    const params = { ejercicio }
    if (filtros.nivel) params.nivel = filtros.nivel
    if (filtros.estatus) params.estatus = filtros.estatus
    if (filtros.criterio) params.criterio = filtros.criterio
    listarHallazgos(contribuyente.id, params)
      .then(setHallazgos)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }

  useEffect(cargarHallazgos, [contribuyente.id, ejercicio, filtros])

  async function handleEjecutar() {
    setEjecutando(true)
    setError(null)
    try {
      const res = await ejecutarAnalisis(contribuyente.id, ejercicio)
      setResultado(res)
      cargarHallazgos()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setEjecutando(false)
    }
  }

  async function handleActualizarEstatus(hallazgoId, payload) {
    const actualizado = await actualizarHallazgo(contribuyente.id, hallazgoId, payload)
    setHallazgos((prev) => prev.map((h) => (h.id === actualizado.id ? actualizado : h)))
    return actualizado
  }

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div className="d-flex gap-2 flex-wrap">
          <select
            className="form-select form-select-sm"
            value={filtros.nivel}
            onChange={(e) => setFiltros((f) => ({ ...f, nivel: e.target.value }))}
          >
            <option value="">Todos los niveles</option>
            {NIVELES.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
          <select
            className="form-select form-select-sm"
            value={filtros.estatus}
            onChange={(e) => setFiltros((f) => ({ ...f, estatus: e.target.value }))}
          >
            <option value="">Todos los estatus</option>
            {ESTATUSES.map((e) => (
              <option key={e} value={e}>
                {e.replace('_', ' ')}
              </option>
            ))}
          </select>
          <input
            type="text"
            className="form-control form-control-sm"
            placeholder="Código de criterio"
            style={{ width: 160 }}
            value={filtros.criterio}
            onChange={(e) => setFiltros((f) => ({ ...f, criterio: e.target.value }))}
          />
        </div>
        <button className="btn btn-primary" onClick={handleEjecutar} disabled={ejecutando}>
          <Play size={14} />
          {ejecutando ? 'Analizando...' : 'Ejecutar análisis'}
        </button>
      </div>

      <ErrorAlert message={error} onClose={() => setError(null)} />

      {resultado && (
        <div className="alert alert-info">
          Análisis completado: {resultado.cfdiAnalizados} CFDI analizados, {resultado.hallazgos} hallazgos generados.
          {resultado.noEvaluados?.length > 0 && (
            <div className="small mt-1">No evaluados: {resultado.noEvaluados.join(', ')}</div>
          )}
        </div>
      )}

      {cargando ? (
        <LoadingSpinner />
      ) : hallazgos.length === 0 ? (
        <div className="card">
          <div className="card-body text-center text-secondary py-5">
            No hay hallazgos para los filtros seleccionados. Ejecuta el análisis para generarlos.
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Identificador</th>
                  <th>Criterio</th>
                  <th>Periodo</th>
                  <th className="text-end"># CFDI</th>
                  <th className="text-end">Importe</th>
                  <th>Nivel</th>
                  <th>Estatus</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {hallazgos.map((h) => (
                  <tr key={h.id} role="button" onClick={() => setHallazgoSeleccionado(h)}>
                    <td className="small font-monospace">{h.identificador}</td>
                    <td className="small">
                      <span className="badge rounded-pill border text-body-secondary font-monospace me-1">{h.codigoCriterio}</span>
                      {h.criterio}
                    </td>
                    <td className="small">{h.periodo}</td>
                    <td className="text-end small">{h.numCfdi}</td>
                    <td className="text-end small">{formatCurrency(h.importe)}</td>
                    <td>
                      <NivelBadge nivel={h.nivel} />
                    </td>
                    <td>
                      <EstatusBadge estatus={h.estatus} />
                    </td>
                    <td>
                      <ChevronRight size={16} className="text-secondary" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <HallazgoDetalleModal
        hallazgo={hallazgoSeleccionado}
        contribuyenteId={contribuyente.id}
        onClose={() => setHallazgoSeleccionado(null)}
        onActualizar={handleActualizarEstatus}
      />
    </div>
  )
}

function HallazgoDetalleModal({ hallazgo, contribuyenteId, onClose, onActualizar }) {
  const [detalle, setDetalle] = useState(null)
  const [estatus, setEstatus] = useState('')
  const [notas, setNotas] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!hallazgo) return
    setDetalle(null)
    setError(null)
    setEstatus(hallazgo.estatus)
    setNotas(hallazgo.notas || '')
    obtenerHallazgo(contribuyenteId, hallazgo.id)
      .then(setDetalle)
      .catch((err) => setError(apiErrorMessage(err)))
  }, [hallazgo, contribuyenteId])

  async function guardar() {
    setGuardando(true)
    setError(null)
    try {
      await onActualizar(hallazgo.id, { estatus, notas })
      onClose()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal show={!!hallazgo} onClose={onClose} title={hallazgo?.identificador} size="lg">
      {!hallazgo ? null : (
        <>
          <ErrorAlert message={error} onClose={() => setError(null)} />
          <h6>
            {hallazgo.codigoCriterio} — {hallazgo.criterio}
          </h6>
          <p className="small">{hallazgo.descripcion}</p>
          <div className="row small mb-2">
            <div className="col-6">
              <strong>Consecuencia:</strong> {hallazgo.consecuencia}
            </div>
            <div className="col-6">
              <strong>Recomendación:</strong> {hallazgo.recomendacion}
            </div>
          </div>

          <div className="row g-2 mb-3">
            <div className="col-md-6">
              <label className="form-label small">Estatus de seguimiento</label>
              <select className="form-select form-select-sm" value={estatus} onChange={(e) => setEstatus(e.target.value)}>
                {ESTATUSES.map((e) => (
                  <option key={e} value={e}>
                    {e.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-6">
              <label className="form-label small">Notas</label>
              <input
                type="text"
                className="form-control form-control-sm"
                maxLength={1000}
                value={notas}
                onChange={(e) => setNotas(e.target.value)}
              />
            </div>
          </div>
          <button className="btn btn-sm btn-primary mb-3" onClick={guardar} disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar seguimiento'}
          </button>

          <h6 className="small">UUIDs involucrados ({detalle?.uuids?.length ?? '...'})</h6>
          {!detalle ? (
            <LoadingSpinner />
          ) : (
            <ul className="small font-monospace" style={{ maxHeight: 200, overflowY: 'auto' }}>
              {detalle.uuids.map((uuid) => (
                <li key={uuid}>{uuid}</li>
              ))}
            </ul>
          )}
        </>
      )}
    </Modal>
  )
}
