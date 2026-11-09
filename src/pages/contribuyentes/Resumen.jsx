import { useEffect, useState } from 'react'
import { Sparkles, SquarePen } from 'lucide-react'
import { editarResumen, generarResumen, obtenerResumen } from '../../api/resumen'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import { formatDateTime } from '../../utils/format'

export default function Resumen() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [resumen, setResumen] = useState(null)
  const [noExiste, setNoExiste] = useState(false)
  const [texto, setTexto] = useState('')
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!ejercicio) return
    setCargando(true)
    setNoExiste(false)
    obtenerResumen(contribuyente.id, ejercicio)
      .then((data) => {
        setResumen(data)
        setTexto(data.texto || '')
      })
      .catch(() => {
        setResumen(null)
        setNoExiste(true)
      })
      .finally(() => setCargando(false))
  }, [contribuyente.id, ejercicio])

  async function handleGenerar() {
    setProcesando(true)
    setError(null)
    try {
      const data = await generarResumen(contribuyente.id, ejercicio)
      setResumen(data)
      setTexto(data.texto || '')
      setNoExiste(false)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setProcesando(false)
    }
  }

  async function handleGuardar() {
    setProcesando(true)
    setError(null)
    try {
      const data = await editarResumen(contribuyente.id, ejercicio, texto)
      setResumen(data)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setProcesando(false)
    }
  }

  if (cargando) return <LoadingSpinner />

  return (
    <div className="card">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start mb-3">
          <h6 className="card-title mb-0">Resumen ejecutivo {ejercicio}</h6>
          <button className="btn btn-outline-primary btn-sm" onClick={handleGenerar} disabled={procesando}>
            <Sparkles size={14} />
            {resumen ? 'Regenerar' : 'Generar'}
          </button>
        </div>

        <ErrorAlert message={error} onClose={() => setError(null)} />

        {noExiste && !resumen ? (
          <div className="text-center text-secondary py-5">
            Aún no se ha generado un resumen ejecutivo para este ejercicio. Genera uno a partir del análisis más
            reciente.
          </div>
        ) : (
          <>
            {resumen?.editado && (
              <div className="small text-secondary mb-2">
                <SquarePen size={14} />
                Editado manualmente · Última actualización {formatDateTime(resumen.actualizadoEn)}
              </div>
            )}
            <textarea
              className="form-control"
              rows={16}
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
            />
            <button className="btn btn-primary mt-3" onClick={handleGuardar} disabled={procesando}>
              {procesando ? 'Guardando...' : 'Guardar edición'}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
