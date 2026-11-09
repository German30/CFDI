import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, IdCard, FolderOpen, Pencil, Trash2 } from 'lucide-react'
import {
  crearContribuyente,
  editarContribuyente,
  eliminarContribuyente,
  listarContribuyentes,
} from '../../api/contribuyentes'
import { apiErrorMessage } from '../../api/client'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'
import ConfirmModal from '../../components/common/ConfirmModal'
import ContribuyenteFormModal from './ContribuyenteFormModal'

export default function ContribuyentesList() {
  const [contribuyentes, setContribuyentes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [modalAbierto, setModalAbierto] = useState(false)
  const [editando, setEditando] = useState(null)
  const [eliminando, setEliminando] = useState(null)

  function cargar() {
    setCargando(true)
    listarContribuyentes()
      .then(setContribuyentes)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [])

  async function handleSubmit(form) {
    if (editando) {
      const actualizado = await editarContribuyente(editando.id, form)
      setContribuyentes((prev) => prev.map((c) => (c.id === actualizado.id ? actualizado : c)))
    } else {
      const creado = await crearContribuyente(form)
      setContribuyentes((prev) => [...prev, creado])
    }
  }

  async function confirmarEliminar() {
    try {
      await eliminarContribuyente(eliminando.id)
      setContribuyentes((prev) => prev.filter((c) => c.id !== eliminando.id))
      setEliminando(null)
    } catch (err) {
      setError(apiErrorMessage(err))
      setEliminando(null)
    }
  }

  return (
    <div>
      <div className="d-flex align-items-center justify-content-between mb-4">
        <h3 className="mb-0">Contribuyentes</h3>
        <button
          className="btn btn-primary"
          onClick={() => {
            setEditando(null)
            setModalAbierto(true)
          }}
        >
          <Plus size={16} /> Nuevo
        </button>
      </div>

      <ErrorAlert message={error} onClose={() => setError(null)} />

      {cargando ? (
        <LoadingSpinner />
      ) : contribuyentes.length === 0 ? (
        <div className="card">
          <div className="card-body text-center text-secondary py-5">
            <IdCard size={40} className="mb-2" style={{ display: 'inline-block' }} />
            Aún no has registrado ningún RFC.
          </div>
        </div>
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>RFC</th>
                  <th>Razón social</th>
                  <th>Ejercicio</th>
                  <th>Régimen fiscal</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {contribuyentes.map((c) => (
                  <tr key={c.id}>
                    <td className="fw-semibold">{c.rfc}</td>
                    <td>{c.razonSocial}</td>
                    <td>{c.ejercicio}</td>
                    <td>{c.regimenFiscal || '—'}</td>
                    <td className="text-end">
                      <Link to={`/contribuyentes/${c.id}/importaciones`} className="btn btn-sm btn-outline-primary me-1">
                        <FolderOpen size={14} /> Abrir
                      </Link>
                      <button
                        className="btn btn-sm btn-outline-secondary me-1"
                        onClick={() => {
                          setEditando(c)
                          setModalAbierto(true)
                        }}
                      >
                        <Pencil size={14} />
                      </button>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => setEliminando(c)}>
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <ContribuyenteFormModal
        show={modalAbierto}
        contribuyente={editando}
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmModal
        show={!!eliminando}
        title="Eliminar contribuyente"
        onConfirm={confirmarEliminar}
        onCancel={() => setEliminando(null)}
      >
        ¿Seguro que deseas eliminar <strong>{eliminando?.rfc}</strong>? Se borrarán todos sus CFDI, hallazgos e
        historial. Esta acción no se puede deshacer.
      </ConfirmModal>
    </div>
  )
}
