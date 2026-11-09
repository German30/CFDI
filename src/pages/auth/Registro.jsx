import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { apiErrorMessage } from '../../api/client'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function Registro() {
  const { registro } = useAuth()
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    if (password.length < 10) {
      setError('La contraseña debe tener al menos 10 caracteres')
      return
    }
    setEnviando(true)
    try {
      await registro(nombre, email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="d-flex align-items-center justify-content-center" style={{ minHeight: '100vh' }}>
      <div className="card" style={{ width: 400 }}>
        <div className="card-body p-4">
          <div className="d-flex align-items-center gap-2 mb-4">
            <span
              className="d-inline-flex align-items-center justify-content-center rounded-2 bg-primary text-white fw-semibold"
              style={{ width: 34, height: 34, fontSize: 14 }}
            >
              CA
            </span>
            <div className="lh-sm">
              <div className="fw-semibold">CFDI Atípicos</div>
              <div className="text-secondary" style={{ fontSize: 12.5 }}>
                Detector de riesgo fiscal
              </div>
            </div>
          </div>

          <h5 className="mb-1">Crea tu cuenta</h5>
          <p className="text-secondary small mb-3">Regístrate para empezar a analizar tus RFC.</p>

          <ErrorAlert message={error} onClose={() => setError(null)} />

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label small text-body-secondary">Nombre completo</label>
              <input
                type="text"
                className="form-control"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="mb-3">
              <label className="form-label small text-body-secondary">Correo electrónico</label>
              <input
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="mb-4">
              <label className="form-label small text-body-secondary">Contraseña</label>
              <input
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={10}
                maxLength={72}
                required
              />
              <div className="form-text">Mínimo 10 caracteres.</div>
            </div>
            <button type="submit" className="btn btn-primary w-100" disabled={enviando}>
              {enviando ? 'Creando cuenta...' : 'Crear cuenta'}
            </button>
          </form>

          <p className="text-center small text-secondary mt-4 mb-0">
            ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
