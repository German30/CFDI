import { useEffect, useRef, useState } from 'react'
import { OctagonAlert } from 'lucide-react'
import { contarLista69b, importarLista69b } from '../api/lista69b'
import { apiErrorMessage } from '../api/client'
import ErrorAlert from '../components/common/ErrorAlert'

export default function Lista69b() {
  const [registros, setRegistros] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [subiendo, setSubiendo] = useState(false)
  const [error, setError] = useState(null)
  const fileInputRef = useRef(null)

  function cargar() {
    setCargando(true)
    contarLista69b()
      .then((data) => setRegistros(data.registros))
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [])

  async function handleUpload(e) {
    const archivo = e.target.files?.[0]
    if (!archivo) return
    setSubiendo(true)
    setError(null)
    try {
      const data = await importarLista69b(archivo)
      setRegistros(data.registros)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubiendo(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  return (
    <div>
      <h3 className="mb-1">Lista 69-B del SAT</h3>
      <p className="text-secondary" style={{ maxWidth: 560 }}>
        Importa el listado de contribuyentes publicados por el SAT conforme al artículo 69-B del CFF (EFOS/EDOS)
        para cruzarlo con tus análisis.
      </p>

      <ErrorAlert message={error} onClose={() => setError(null)} />

      <div className="card" style={{ maxWidth: 560 }}>
        <div className="card-body d-flex align-items-center gap-3 py-4">
          <span
            className="d-inline-flex align-items-center justify-content-center rounded-2 bg-warning-subtle text-warning-emphasis flex-shrink-0"
            style={{ width: 44, height: 44, fontSize: 18 }}
          >
            <OctagonAlert size={20} />
          </span>
          <div className="flex-grow-1">
            <div className="fs-3 fw-semibold lh-sm">{cargando ? '—' : registros?.toLocaleString('es-MX')}</div>
            <div className="small text-secondary">registros importados</div>
          </div>
          <label className="btn btn-outline-secondary mb-0 flex-shrink-0">
            {subiendo ? 'Subiendo...' : 'Importar CSV'}
            <input ref={fileInputRef} type="file" accept=".csv" hidden disabled={subiendo} onChange={handleUpload} />
          </label>
        </div>
      </div>
    </div>
  )
}
