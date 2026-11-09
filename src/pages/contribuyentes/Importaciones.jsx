import { useCallback, useEffect, useRef, useState } from 'react'
import { FileCode, FileSpreadsheet } from 'lucide-react'
import {
  listarErroresImportacion,
  listarImportaciones,
  subirMetadata,
  subirXml,
} from '../../api/importaciones'
import { apiErrorMessage } from '../../api/client'
import { useContribuyenteActual } from '../../context/ContribuyenteContext'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import Modal from '../../components/common/Modal'
import { EstadoImportacionBadge } from '../../components/common/badges'
import { formatDateTime } from '../../utils/format'

const JOBS_ACTIVOS = ['EN_COLA', 'PROCESANDO']

export default function Importaciones() {
  const { contribuyente } = useContribuyenteActual()
  const [jobs, setJobs] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [subiendoXml, setSubiendoXml] = useState(false)
  const [subiendoMeta, setSubiendoMeta] = useState(false)
  const [jobErrores, setJobErrores] = useState(null)
  const xmlInputRef = useRef(null)
  const metaInputRef = useRef(null)

  const cargarJobs = useCallback(() => {
    return listarImportaciones(contribuyente.id)
      .then(setJobs)
      .catch((err) => setError(apiErrorMessage(err)))
  }, [contribuyente.id])

  useEffect(() => {
    setCargando(true)
    cargarJobs().finally(() => setCargando(false))
  }, [cargarJobs])

  useEffect(() => {
    const hayActivos = jobs.some((j) => JOBS_ACTIVOS.includes(j.estado))
    if (!hayActivos) return
    const interval = setInterval(cargarJobs, 2000)
    return () => clearInterval(interval)
  }, [jobs, cargarJobs])

  async function handleUpload(tipo, files) {
    if (!files || files.length === 0) return
    setError(null)
    const setter = tipo === 'xml' ? setSubiendoXml : setSubiendoMeta
    setter(true)
    try {
      const fn = tipo === 'xml' ? subirXml : subirMetadata
      const job = await fn(contribuyente.id, Array.from(files))
      setJobs((prev) => [job, ...prev])
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setter(false)
      if (xmlInputRef.current) xmlInputRef.current.value = ''
      if (metaInputRef.current) metaInputRef.current.value = ''
    }
  }

  return (
    <div>
      <ErrorAlert message={error} onClose={() => setError(null)} />

      <div className="row g-3 mb-4">
        <div className="col-md-6">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-title">
                <FileCode size={16} className="me-2" />
                Cargar XML de CFDI
              </h6>
              <p className="small text-secondary">Selecciona uno o varios archivos .xml, o un .zip con varios.</p>
              <input
                ref={xmlInputRef}
                type="file"
                className="form-control"
                multiple
                accept=".xml,.zip"
                disabled={subiendoXml}
                onChange={(e) => handleUpload('xml', e.target.files)}
              />
              {subiendoXml && <div className="small text-secondary mt-2">Subiendo...</div>}
            </div>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-title">
                <FileSpreadsheet size={16} className="me-2" />
                Cargar metadata del SAT
              </h6>
              <p className="small text-secondary">Selecciona archivos .txt de metadata, o un .zip con varios.</p>
              <input
                ref={metaInputRef}
                type="file"
                className="form-control"
                multiple
                accept=".txt,.zip"
                disabled={subiendoMeta}
                onChange={(e) => handleUpload('metadata', e.target.files)}
              />
              {subiendoMeta && <div className="small text-secondary mt-2">Subiendo...</div>}
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <h6 className="card-title mb-3">Historial de importaciones</h6>
          {cargando ? (
            <LoadingSpinner />
          ) : jobs.length === 0 ? (
            <div className="text-center text-secondary py-4">Aún no se han importado archivos.</div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th>Estado</th>
                    <th className="text-end">Total</th>
                    <th className="text-end">Procesados</th>
                    <th className="text-end">Importados</th>
                    <th className="text-end">Duplicados</th>
                    <th className="text-end">Errores</th>
                    <th>Creado</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => (
                    <tr key={job.id}>
                      <td>{job.tipo}</td>
                      <td>
                        <EstadoImportacionBadge estado={job.estado} />
                      </td>
                      <td className="text-end">{job.total}</td>
                      <td className="text-end">{job.procesados}</td>
                      <td className="text-end">{job.importados}</td>
                      <td className="text-end">{job.duplicados}</td>
                      <td className="text-end">{job.errores}</td>
                      <td className="small text-secondary">{formatDateTime(job.creadoEn)}</td>
                      <td>
                        {job.errores > 0 && (
                          <button className="btn btn-sm btn-outline-danger" onClick={() => setJobErrores(job)}>
                            Ver errores
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <ErroresModal job={jobErrores} contribuyenteId={contribuyente.id} onClose={() => setJobErrores(null)} />
    </div>
  )
}

function ErroresModal({ job, contribuyenteId, onClose }) {
  const [pagina, setPagina] = useState(0)
  const [data, setData] = useState(null)
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    if (!job) return
    setCargando(true)
    listarErroresImportacion(contribuyenteId, job.id, pagina, 20)
      .then(setData)
      .finally(() => setCargando(false))
  }, [job, contribuyenteId, pagina])

  useEffect(() => {
    if (job) setPagina(0)
  }, [job])

  return (
    <Modal show={!!job} onClose={onClose} title="Errores de importación" size="lg">
      {cargando || !data ? (
        <LoadingSpinner />
      ) : (
        <>
          <table className="table table-sm">
            <thead>
              <tr>
                <th>Archivo</th>
                <th>Clase</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {(data.content || []).map((e) => (
                <tr key={e.id}>
                  <td className="small">{e.archivo}</td>
                  <td>
                    <span className="badge rounded-pill bg-secondary-subtle text-secondary-emphasis">{e.clase}</span>
                  </td>
                  <td className="small">{e.detalle}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="d-flex justify-content-between align-items-center">
            <button
              className="btn btn-sm btn-outline-secondary"
              disabled={pagina === 0}
              onClick={() => setPagina((p) => p - 1)}
            >
              Anterior
            </button>
            <span className="small text-secondary">Página {pagina + 1}</span>
            <button
              className="btn btn-sm btn-outline-secondary"
              disabled={(data.content || []).length < 20}
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
