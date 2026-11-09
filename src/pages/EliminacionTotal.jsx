import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TriangleAlert } from 'lucide-react'
import { confirmarEliminacionTotal, solicitarEliminacionTotal } from '../api/datos'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import ErrorAlert from '../components/common/ErrorAlert'

const FRASE_REQUERIDA = 'ELIMINAR TODO'

export default function EliminacionTotal() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [solicitud, setSolicitud] = useState(null)
  const [restante, setRestante] = useState(0)
  const [fraseEscrita, setFraseEscrita] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!solicitud) return
    setRestante(solicitud.vigenciaSegundos)
    const interval = setInterval(() => {
      setRestante((r) => {
        if (r <= 1) {
          clearInterval(interval)
          setSolicitud(null)
          return 0
        }
        return r - 1
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [solicitud])

  async function handleSolicitar() {
    setCargando(true)
    setError(null)
    try {
      const data = await solicitarEliminacionTotal()
      setSolicitud(data)
      setFraseEscrita('')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  async function handleConfirmar() {
    setCargando(true)
    setError(null)
    try {
      await confirmarEliminacionTotal(solicitud.token)
      logout()
      navigate('/login', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h3 className="mb-4 text-danger">Zona de riesgo</h3>

      <div className="card border-danger-subtle" style={{ maxWidth: 560 }}>
        <div className="card-body">
          <h6 className="card-title text-danger">
            <TriangleAlert size={16} className="me-2" />
            Eliminar todos mis datos
          </h6>
          <p className="small text-secondary">
            Esto borra permanentemente TODOS tus contribuyentes, CFDI, hallazgos, respaldos y configuración. No se
            puede deshacer.
          </p>

          <ErrorAlert message={error} onClose={() => setError(null)} />

          {!solicitud ? (
            <button className="btn btn-danger" onClick={handleSolicitar} disabled={cargando}>
              {cargando ? 'Solicitando...' : 'Solicitar eliminación total'}
            </button>
          ) : (
            <>
              <div className="alert alert-warning">
                Token de confirmación generado. Vigente por <strong>{restante}s</strong>. Escribe exactamente{' '}
                <strong>{FRASE_REQUERIDA}</strong> para confirmar.
              </div>
              <input
                type="text"
                className="form-control mb-3"
                value={fraseEscrita}
                onChange={(e) => setFraseEscrita(e.target.value)}
                placeholder={FRASE_REQUERIDA}
                disabled={restante === 0}
              />
              <div className="d-flex gap-2">
                <button className="btn btn-outline-secondary" onClick={() => setSolicitud(null)}>
                  Cancelar
                </button>
                <button
                  className="btn btn-danger"
                  disabled={fraseEscrita !== FRASE_REQUERIDA || restante === 0 || cargando}
                  onClick={handleConfirmar}
                >
                  {cargando ? 'Eliminando...' : 'Eliminar todo permanentemente'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
