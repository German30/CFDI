import { useEffect, useState } from 'react'
import { ajustarCriterio, listarCriterios } from '../../api/criterios'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function Criterios() {
  const { contribuyente } = useContribuyenteActual()
  const [criterios, setCriterios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setCargando(true)
    listarCriterios(contribuyente.id)
      .then(setCriterios)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [contribuyente.id])

  async function handleAjuste(codigo, ajuste) {
    try {
      const actualizados = await ajustarCriterio(contribuyente.id, codigo, ajuste)
      setCriterios(actualizados)
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  if (cargando) return <LoadingSpinner />

  const categorias = [...new Set(criterios.map((c) => c.categoria))]

  return (
    <div>
      <ErrorAlert message={error} onClose={() => setError(null)} />
      <p className="text-secondary small">
        Activa o desactiva criterios y ajusta sus parámetros según el perfil de riesgo de este contribuyente.
      </p>

      <div className="accordion" id="acordeonCriterios">
        {categorias.map((categoria) => (
          <div key={categoria} className="mb-3">
            <h6 className="text-secondary text-uppercase small">{categoria}</h6>
            {criterios
              .filter((c) => c.categoria === categoria)
              .map((criterio) => (
                <CriterioItem key={criterio.codigo} criterio={criterio} onAjuste={handleAjuste} />
              ))}
          </div>
        ))}
      </div>
    </div>
  )
}

function CriterioItem({ criterio, onAjuste }) {
  const [parametros, setParametros] = useState(criterio.parametros || {})
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    setParametros(criterio.parametros || {})
  }, [criterio.parametros])

  async function toggleActivo() {
    await onAjuste(criterio.codigo, { activo: !criterio.activo })
  }

  async function guardarParametros() {
    setGuardando(true)
    try {
      await onAjuste(criterio.codigo, { parametros })
    } finally {
      setGuardando(false)
    }
  }

  function restaurarDefaults() {
    setParametros(criterio.parametrosPorDefecto || {})
  }

  const parametrosKeys = Object.keys(criterio.parametrosPorDefecto || criterio.parametros || {})

  return (
    <div className={`card mb-2  ${criterio.activo ? '' : 'opacity-75'}`}>
      <div className="card-body py-2">
        <div className="d-flex align-items-start gap-3">
          <div className="form-check form-switch pt-1">
            <input
              className="form-check-input"
              type="checkbox"
              checked={criterio.activo}
              onChange={toggleActivo}
              role="switch"
            />
          </div>
          <div className="flex-grow-1">
            <div className="d-flex align-items-center gap-2">
              <span className="badge rounded-pill border text-body-secondary font-monospace">{criterio.codigo}</span>
              <span className="fw-semibold">{criterio.nombre}</span>
            </div>
            <div className="small text-secondary mt-1">{criterio.fundamento}</div>
            <details className="small mt-1">
              <summary className="text-primary" style={{ cursor: 'pointer' }}>
                Consecuencia y recomendación
              </summary>
              <div className="mt-1">
                <strong>Consecuencia:</strong> {criterio.consecuencia}
              </div>
              <div>
                <strong>Recomendación:</strong> {criterio.recomendacion}
              </div>
            </details>

            {parametrosKeys.length > 0 && (
              <div className="d-flex flex-wrap gap-2 align-items-end mt-2">
                {parametrosKeys.map((key) => (
                  <div key={key}>
                    <label className="form-label small mb-0">{key}</label>
                    <input
                      type="number"
                      step="any"
                      className="form-control form-control-sm"
                      style={{ width: 110 }}
                      value={parametros[key] ?? ''}
                      onChange={(e) => setParametros((p) => ({ ...p, [key]: Number(e.target.value) }))}
                    />
                  </div>
                ))}
                <button className="btn btn-sm btn-outline-secondary" onClick={restaurarDefaults}>
                  Restaurar
                </button>
                <button className="btn btn-sm btn-primary" onClick={guardarParametros} disabled={guardando}>
                  {guardando ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
