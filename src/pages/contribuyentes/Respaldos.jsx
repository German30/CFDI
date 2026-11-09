import { useEffect, useRef, useState } from 'react'
import { Download, Save, Upload } from 'lucide-react'
import {
  crearRespaldo,
  descargarRespaldoActual,
  descargarRespaldoVersion,
  obtenerEstadoRespaldos,
  restaurarDesdeArchivo,
  restaurarDesdeVersion,
} from '../../api/respaldos'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import ConfirmModal from '../../components/common/ConfirmModal'
import { formatDateTime } from '../../utils/format'

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const unidades = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let v = bytes
  while (v >= 1024 && i < unidades.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(1)} ${unidades[i]}`
}

export default function Respaldos() {
  const { contribuyente } = useContribuyenteActual()
  const [estado, setEstado] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [creando, setCreando] = useState(false)
  const [restaurando, setRestaurando] = useState(null)
  const fileInputRef = useRef(null)

  function cargar() {
    setCargando(true)
    obtenerEstadoRespaldos(contribuyente.id)
      .then(setEstado)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [contribuyente.id])

  async function handleCrear() {
    setCreando(true)
    setError(null)
    try {
      await crearRespaldo(contribuyente.id)
      cargar()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setCreando(false)
    }
  }

  async function handleRestaurarArchivo(e) {
    const archivo = e.target.files?.[0]
    if (!archivo) return
    setError(null)
    try {
      await restaurarDesdeArchivo(contribuyente.id, archivo)
      cargar()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function confirmarRestaurarVersion() {
    try {
      await restaurarDesdeVersion(contribuyente.id, restaurando.id)
      setRestaurando(null)
      cargar()
    } catch (err) {
      setError(apiErrorMessage(err))
      setRestaurando(null)
    }
  }

  if (cargando) return <LoadingSpinner />

  return (
    <div>
      <ErrorAlert message={error} onClose={() => setError(null)} />

      <div className="card mb-3">
        <div className="card-body d-flex flex-wrap gap-2 align-items-center justify-content-between">
          <div>
            <div className="small text-secondary">Último respaldo</div>
            <div className="fw-semibold">{formatDateTime(estado?.ultimoRespaldo)}</div>
          </div>
          <div className="d-flex gap-2">
            <button className="btn btn-outline-secondary" onClick={() => descargarRespaldoActual(contribuyente.id)}>
              <Download size={14} /> Descargar estado actual
            </button>
            <button className="btn btn-primary" onClick={handleCrear} disabled={creando}>
              <Save size={14} />
              {creando ? 'Creando...' : 'Crear respaldo manual'}
            </button>
            <label className="btn btn-outline-warning mb-0">
              <Upload size={14} /> Restaurar desde archivo
              <input ref={fileInputRef} type="file" accept=".json" hidden onChange={handleRestaurarArchivo} />
            </label>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <h6 className="card-title mb-3">Versiones disponibles</h6>
          {(estado?.versiones || []).length === 0 ? (
            <div className="text-center text-secondary py-4">No hay versiones de respaldo todavía.</div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>Creado</th>
                    <th>Origen</th>
                    <th>Tamaño</th>
                    <th className="text-end">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {estado.versiones.map((v) => (
                    <tr key={v.id}>
                      <td>{formatDateTime(v.creadoEn)}</td>
                      <td>
                        <span
                          className={`badge rounded-pill ${v.origen === 'MANUAL' ? 'bg-primary-subtle text-primary-emphasis' : 'bg-secondary-subtle text-secondary-emphasis'}`}
                        >
                          {v.origen}
                        </span>
                      </td>
                      <td>{formatBytes(v.tamanoBytes)}</td>
                      <td className="text-end">
                        <button
                          className="btn btn-sm btn-outline-secondary me-1"
                          onClick={() => descargarRespaldoVersion(contribuyente.id, v.id)}
                        >
                          <Download size={14} />
                        </button>
                        <button className="btn btn-sm btn-outline-warning" onClick={() => setRestaurando(v)}>
                          Restaurar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <ConfirmModal
        show={!!restaurando}
        title="Restaurar versión de respaldo"
        confirmLabel="Restaurar"
        confirmVariant="warning"
        onConfirm={confirmarRestaurarVersion}
        onCancel={() => setRestaurando(null)}
      >
        Esto reemplazará todos los datos actuales del contribuyente con el respaldo del{' '}
        <strong>{formatDateTime(restaurando?.creadoEn)}</strong>. Esta acción no se puede deshacer.
      </ConfirmModal>
    </div>
  )
}
