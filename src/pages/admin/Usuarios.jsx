import { useEffect, useState } from 'react'
import { cambiarActivoUsuario, listarUsuarios } from '../../api/admin'
import { apiErrorMessage } from '../../api/client'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    listarUsuarios()
      .then(setUsuarios)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setCargando(false))
  }, [])

  async function toggleActivo(usuario) {
    try {
      const actualizado = await cambiarActivoUsuario(usuario.id, !usuario.activo)
      setUsuarios((prev) => prev.map((u) => (u.id === actualizado.id ? actualizado : u)))
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  if (cargando) return <LoadingSpinner />

  return (
    <div>
      <h3 className="mb-4">Usuarios</h3>
      <ErrorAlert message={error} onClose={() => setError(null)} />

      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Email</th>
                <th>Rol</th>
                <th>Activo</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id}>
                  <td>{u.nombre}</td>
                  <td>{u.email}</td>
                  <td>
                    <span
                      className={`badge rounded-pill ${u.rol === 'ADMIN' ? 'bg-primary-subtle text-primary-emphasis' : 'bg-secondary-subtle text-secondary-emphasis'}`}
                    >
                      {u.rol}
                    </span>
                  </td>
                  <td>
                    <div className="form-check form-switch mb-0">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        checked={u.activo}
                        onChange={() => toggleActivo(u)}
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
