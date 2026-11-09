import { useEffect, useState } from 'react'
import { obtenerMatriz } from '../../api/analisis'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import Modal from '../../components/common/Modal'
import { NivelBadge } from '../../components/common/badges'
import { formatCurrency } from '../../utils/format'

const NIVEL_COLOR = {
  BAJO: 'rgba(21, 128, 61, 0.12)',
  MODERADO: 'rgba(180, 83, 9, 0.12)',
  ALTO: 'rgba(194, 65, 12, 0.14)',
  CRITICO: 'rgba(185, 28, 28, 0.14)',
}

export default function Matriz() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [matriz, setMatriz] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [celdaSeleccionada, setCeldaSeleccionada] = useState(null)

  useEffect(() => {
    if (!ejercicio) return
    setCargando(true)
    obtenerMatriz(contribuyente.id, ejercicio)
      .then(setMatriz)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [contribuyente.id, ejercicio])

  if (cargando) return <LoadingSpinner />
  if (error) return <ErrorAlert message={error} />
  if (!matriz) return null

  function celdaEn(probabilidad, impacto) {
    return matriz.celdas.find((c) => c.probabilidad === probabilidad && c.impacto === impacto)
  }

  return (
    <div>
      <div className="row g-3 mb-4">
        {Object.entries(matriz.porNivel || {}).map(([nivel, cantidad]) => (
          <div className="col-6 col-md-3" key={nivel}>
            <div className="card h-100">
              <div className="card-body">
                <NivelBadge nivel={nivel} />
                <div className="fs-4 fw-semibold mt-1">{cantidad}</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-body">
          <h6 className="card-title mb-3">Matriz de riesgo (probabilidad × impacto)</h6>
          <div className="table-responsive">
            <table className="table table-bordered text-center mb-0" style={{ tableLayout: 'fixed' }}>
              <thead>
                <tr>
                  <th style={{ width: 120 }}>Probabilidad \ Impacto</th>
                  {[1, 2, 3, 4, 5].map((i) => (
                    <th key={i}>{i}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {[5, 4, 3, 2, 1].map((p) => (
                  <tr key={p}>
                    <th className="bg-light">{p}</th>
                    {[1, 2, 3, 4, 5].map((i) => {
                      const celda = celdaEn(p, i)
                      return (
                        <td
                          key={i}
                          role={celda ? 'button' : undefined}
                          className={celda ? 'cell-interactive' : ''}
                          style={{
                            backgroundColor: celda ? NIVEL_COLOR[celda.nivel] : '#fff',
                            cursor: celda ? 'pointer' : 'default',
                            height: 72,
                          }}
                          onClick={() => celda && setCeldaSeleccionada(celda)}
                        >
                          {celda && (
                            <>
                              <div className="fw-semibold">{celda.hallazgos}</div>
                              <div className="small text-secondary">{formatCurrency(celda.importe)}</div>
                            </>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <Modal
        show={!!celdaSeleccionada}
        onClose={() => setCeldaSeleccionada(null)}
        title={celdaSeleccionada ? `Probabilidad ${celdaSeleccionada.probabilidad} · Impacto ${celdaSeleccionada.impacto}` : ''}
      >
        {celdaSeleccionada && (
          <>
            <NivelBadge nivel={celdaSeleccionada.nivel} />
            <p className="small mt-2">Importe total: {formatCurrency(celdaSeleccionada.importe)}</p>
            <h6 className="small">Hallazgos ({celdaSeleccionada.identificadores.length})</h6>
            <ul className="small">
              {celdaSeleccionada.identificadores.map((id) => (
                <li key={id}>{id}</li>
              ))}
            </ul>
          </>
        )}
      </Modal>
    </div>
  )
}
