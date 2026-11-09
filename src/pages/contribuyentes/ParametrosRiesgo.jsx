import { useEffect, useState } from 'react'
import { actualizarParametrosRiesgo, obtenerParametrosRiesgo } from '../../api/analisis'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function ParametrosRiesgo() {
  const { contribuyente } = useContribuyenteActual()
  const [params, setParams] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(null)
  const [exito, setExito] = useState(false)

  useEffect(() => {
    setCargando(true)
    obtenerParametrosRiesgo(contribuyente.id)
      .then(setParams)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [contribuyente.id])

  function setCampo(campo, valor) {
    setParams((p) => ({ ...p, [campo]: valor }))
    setExito(false)
  }

  function setArrayCampo(campo, index, valor) {
    setParams((p) => {
      const arr = [...(p[campo] || [])]
      arr[index] = valor
      return { ...p, [campo]: arr }
    })
    setExito(false)
  }

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true)
    setError(null)
    try {
      const actualizado = await actualizarParametrosRiesgo(contribuyente.id, params)
      setParams(actualizado)
      setExito(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <LoadingSpinner />
  if (!params) return <ErrorAlert message={error} />

  return (
    <div className="card">
      <div className="card-body">
        <h6 className="card-title mb-3">Ponderaciones del cálculo de riesgo</h6>
        <ErrorAlert message={error} onClose={() => setError(null)} />
        {exito && <div className="alert alert-success">Parámetros actualizados.</div>}

        <form onSubmit={guardar} style={{ maxWidth: 700 }}>
          <h6 className="small text-secondary text-uppercase mt-2">Pesos</h6>
          <div className="row g-2 mb-3">
            {['pesoIntensidad', 'pesoRecurrencia', 'pesoImporte', 'pesoRelativo'].map((campo) => (
              <div className="col-6 col-md-3" key={campo}>
                <label className="form-label small">{campo}</label>
                <input
                  type="number"
                  step="0.01"
                  className="form-control form-control-sm"
                  value={params[campo] ?? ''}
                  onChange={(e) => setCampo(campo, Number(e.target.value))}
                />
              </div>
            ))}
          </div>

          <h6 className="small text-secondary text-uppercase">Cortes de importe</h6>
          <div className="row g-2 mb-3">
            {(params.cortesImporte || []).map((valor, i) => (
              <div className="col-6 col-md-3" key={i}>
                <label className="form-label small">Corte {i + 1}</label>
                <input
                  type="number"
                  className="form-control form-control-sm"
                  value={valor}
                  onChange={(e) => setArrayCampo('cortesImporte', i, Number(e.target.value))}
                />
              </div>
            ))}
          </div>

          <h6 className="small text-secondary text-uppercase">Cortes de porcentaje</h6>
          <div className="row g-2 mb-3">
            {(params.cortesPct || []).map((valor, i) => (
              <div className="col-6 col-md-3" key={i}>
                <label className="form-label small">Corte {i + 1} (%)</label>
                <input
                  type="number"
                  step="0.1"
                  className="form-control form-control-sm"
                  value={valor}
                  onChange={(e) => setArrayCampo('cortesPct', i, Number(e.target.value))}
                />
              </div>
            ))}
          </div>

          <h6 className="small text-secondary text-uppercase">Umbrales de nivel</h6>
          <div className="row g-2 mb-3">
            {['umbralModerado', 'umbralAlto', 'umbralCritico'].map((campo) => (
              <div className="col-6 col-md-4" key={campo}>
                <label className="form-label small">{campo}</label>
                <input
                  type="number"
                  step="0.1"
                  className="form-control form-control-sm"
                  value={params[campo] ?? ''}
                  onChange={(e) => setCampo(campo, Number(e.target.value))}
                />
              </div>
            ))}
          </div>

          <button type="submit" className="btn btn-primary" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar ponderaciones'}
          </button>
        </form>
      </div>
    </div>
  )
}
