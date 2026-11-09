import { useEffect, useState } from 'react'
import { obtenerMalla, obtenerMallaDetalle } from '../../api/malla'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import Modal from '../../components/common/Modal'
import { formatCurrency, formatDate } from '../../utils/format'

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']

const ATIPICIDAD_COLOR = {
  0: '#ffffff',
  1: 'rgba(180, 83, 9, 0.12)',
  2: 'rgba(194, 65, 12, 0.18)',
  3: 'rgba(185, 28, 28, 0.2)',
}

export default function Malla() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [filtros, setFiltros] = useState({ tipo: '', direccion: '', contraparte: '', claveProdServ: '' })
  const [malla, setMalla] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [celdaSeleccionada, setCeldaSeleccionada] = useState(null)

  function paramsActivos() {
    const p = { ejercicio }
    if (filtros.tipo) p.tipo = filtros.tipo
    if (filtros.direccion) p.direccion = filtros.direccion
    if (filtros.contraparte) p.contraparte = filtros.contraparte
    if (filtros.claveProdServ) p.claveProdServ = filtros.claveProdServ
    return p
  }

  useEffect(() => {
    if (!ejercicio) return
    setCargando(true)
    obtenerMalla(contribuyente.id, paramsActivos())
      .then(setMalla)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contribuyente.id, ejercicio, filtros])

  return (
    <div>
      <div className="card mb-3">
        <div className="card-body d-flex flex-wrap gap-2 align-items-end">
          <div>
            <label className="form-label small mb-1">Tipo</label>
            <select
              className="form-select form-select-sm"
              value={filtros.tipo}
              onChange={(e) => setFiltros((f) => ({ ...f, tipo: e.target.value }))}
            >
              <option value="">Todos</option>
              <option value="I">Ingreso</option>
              <option value="E">Egreso</option>
              <option value="N">Nómina</option>
              <option value="P">Pago</option>
              <option value="T">Traslado</option>
            </select>
          </div>
          <div>
            <label className="form-label small mb-1">Dirección</label>
            <select
              className="form-select form-select-sm"
              value={filtros.direccion}
              onChange={(e) => setFiltros((f) => ({ ...f, direccion: e.target.value }))}
            >
              <option value="">Ambas</option>
              <option value="EMITIDO">Emitido</option>
              <option value="RECIBIDO">Recibido</option>
            </select>
          </div>
          <div>
            <label className="form-label small mb-1">Contraparte (RFC)</label>
            <input
              type="text"
              className="form-control form-control-sm"
              value={filtros.contraparte}
              onChange={(e) => setFiltros((f) => ({ ...f, contraparte: e.target.value.toUpperCase() }))}
            />
          </div>
          <div>
            <label className="form-label small mb-1">Clave prod/serv</label>
            <input
              type="text"
              className="form-control form-control-sm"
              value={filtros.claveProdServ}
              onChange={(e) => setFiltros((f) => ({ ...f, claveProdServ: e.target.value }))}
            />
          </div>
          <div className="ms-auto d-flex gap-2 small">
            <LeyendaAtipicidad />
          </div>
        </div>
      </div>

      <ErrorAlert message={error} onClose={() => setError(null)} />

      {cargando ? (
        <LoadingSpinner />
      ) : !malla || malla.filas.length === 0 ? (
        <div className="text-center text-secondary py-5">Sin datos para los filtros seleccionados.</div>
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="table table-bordered align-middle mb-0 text-center">
              <thead>
                <tr>
                  <th className="text-start">Tipo / Dirección</th>
                  {MESES.map((m) => (
                    <th key={m}>{m}</th>
                  ))}
                  <th>Total</th>
                  <th>% cancel.</th>
                </tr>
              </thead>
              <tbody>
                {malla.filas.map((fila) => (
                  <tr key={`${fila.tipo}-${fila.direccion}`}>
                    <td className="text-start small fw-semibold">
                      {fila.tipo} · {fila.direccion}
                    </td>
                    {fila.celdas.map((celda) => (
                      <td
                        key={celda.mes}
                        role="button"
                        className="cell-interactive"
                        style={{ backgroundColor: ATIPICIDAD_COLOR[celda.atipicidad] ?? '#fff', cursor: 'pointer' }}
                        onClick={() =>
                          setCeldaSeleccionada({ fila, celda })
                        }
                        title={`Variación ${celda.variacionPct?.toFixed(1)}% · z=${celda.z?.toFixed(2)}`}
                      >
                        <div className="small fw-semibold">{formatCurrency(celda.importe)}</div>
                        <div className="small text-secondary">{celda.operaciones} op.</div>
                      </td>
                    ))}
                    <td className="small fw-semibold">{formatCurrency(fila.totalImporte)}</td>
                    <td className="small">{fila.pctCancelacion?.toFixed(1)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <DetalleCeldaModal
        contribuyenteId={contribuyente.id}
        seleccion={celdaSeleccionada}
        filtros={paramsActivos()}
        onClose={() => setCeldaSeleccionada(null)}
      />
    </div>
  )
}

function LeyendaAtipicidad() {
  return (
    <div className="d-flex align-items-center gap-1 small text-secondary">
      Atipicidad:
      {[0, 1, 2, 3].map((n) => (
        <span
          key={n}
          className="d-inline-block border"
          style={{ width: 16, height: 16, backgroundColor: ATIPICIDAD_COLOR[n] }}
          title={`Nivel ${n}`}
        ></span>
      ))}
    </div>
  )
}

function DetalleCeldaModal({ contribuyenteId, seleccion, filtros, onClose }) {
  const [data, setData] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    if (!seleccion) return
    setPagina(0)
  }, [seleccion])

  useEffect(() => {
    if (!seleccion) return
    setCargando(true)
    obtenerMallaDetalle(contribuyenteId, {
      ...filtros,
      tipo: seleccion.fila.tipo,
      direccion: seleccion.fila.direccion,
      mes: seleccion.celda.mes,
      pagina,
      tamano: 50,
    })
      .then(setData)
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seleccion, pagina])

  const titulo = seleccion
    ? `${seleccion.fila.tipo} · ${seleccion.fila.direccion} · ${MESES[seleccion.celda.mes - 1] || seleccion.celda.mes}`
    : ''

  return (
    <Modal show={!!seleccion} onClose={onClose} title={titulo} size="lg">
      {cargando || !data ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="table-responsive">
            <table className="table table-sm table-hover">
              <thead>
                <tr>
                  <th>UUID</th>
                  <th>Fecha</th>
                  <th>Contraparte</th>
                  <th className="text-end">Importe</th>
                  <th>Cancelado</th>
                </tr>
              </thead>
              <tbody>
                {data.elementos.map((e) => (
                  <tr key={e.uuid}>
                    <td className="small font-monospace">{e.uuid}</td>
                    <td className="small">{formatDate(e.fecha)}</td>
                    <td className="small">
                      {e.rfcContraparte}
                      <br />
                      <span className="text-secondary">{e.nombreContraparte}</span>
                    </td>
                    <td className="text-end small">{formatCurrency(e.importe)}</td>
                    <td>{e.cancelado && <span className="badge rounded-pill bg-danger-subtle text-danger-emphasis">Cancelado</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="d-flex justify-content-between align-items-center">
            <button className="btn btn-sm btn-outline-secondary" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
              Anterior
            </button>
            <span className="small text-secondary">
              Página {data.pagina + 1} · {data.total} registros
            </span>
            <button
              className="btn btn-sm btn-outline-secondary"
              disabled={(pagina + 1) * data.tamano >= data.total}
              onClick={() => setPagina((p) => p + 1)}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
    </Modal>
  )
}
