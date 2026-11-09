import { useState } from 'react'
import { FileText, FileSpreadsheet } from 'lucide-react'
import { exportarExcel, exportarPdf } from '../../api/exportacion'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function Exportacion() {
  const { contribuyente, ejercicio } = useContribuyenteActual()
  const [descargandoPdf, setDescargandoPdf] = useState(false)
  const [descargandoExcel, setDescargandoExcel] = useState(false)
  const [error, setError] = useState(null)

  async function handlePdf() {
    setDescargandoPdf(true)
    setError(null)
    try {
      await exportarPdf(contribuyente.id, ejercicio)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setDescargandoPdf(false)
    }
  }

  async function handleExcel() {
    setDescargandoExcel(true)
    setError(null)
    try {
      await exportarExcel(contribuyente.id, ejercicio)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setDescargandoExcel(false)
    }
  }

  return (
    <div>
      <ErrorAlert message={error} onClose={() => setError(null)} />

      <div className="card" style={{ maxWidth: 640 }}>
        <ul className="list-group list-group-flush">
          <li className="list-group-item d-flex align-items-center gap-3 py-3">
            <span
              className="d-inline-flex align-items-center justify-content-center rounded-2 bg-danger-subtle text-danger-emphasis flex-shrink-0"
              style={{ width: 36, height: 36 }}
            >
              <FileText size={16} />
            </span>
            <div className="flex-grow-1">
              <div className="fw-semibold" style={{ fontSize: 14.5 }}>
                Resumen en PDF
              </div>
              <div className="text-secondary small">Documento ejecutivo del ejercicio {ejercicio}.</div>
            </div>
            <button className="btn btn-sm btn-outline-secondary" onClick={handlePdf} disabled={descargandoPdf}>
              {descargandoPdf ? 'Descargando...' : 'Descargar'}
            </button>
          </li>
          <li className="list-group-item d-flex align-items-center gap-3 py-3">
            <span
              className="d-inline-flex align-items-center justify-content-center rounded-2 bg-success-subtle text-success-emphasis flex-shrink-0"
              style={{ width: 36, height: 36 }}
            >
              <FileSpreadsheet size={16} />
            </span>
            <div className="flex-grow-1">
              <div className="fw-semibold" style={{ fontSize: 14.5 }}>
                Análisis en Excel
              </div>
              <div className="text-secondary small">Libro con 5 hojas de detalle del ejercicio {ejercicio}.</div>
            </div>
            <button className="btn btn-sm btn-outline-secondary" onClick={handleExcel} disabled={descargandoExcel}>
              {descargandoExcel ? 'Descargando...' : 'Descargar'}
            </button>
          </li>
        </ul>
      </div>
    </div>
  )
}
