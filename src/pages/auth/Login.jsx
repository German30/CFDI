import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { apiErrorMessage } from '../../api/client'
import ErrorAlert from '../../components/common/ErrorAlert'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await login(email, password)
      navigate(location.state?.from?.pathname || '/', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="d-flex align-items-center justify-content-center" style={{ minHeight: '100vh' }}>
      <div className="card" style={{ width: 380 }}>
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

          <h5 className="mb-1">Inicia sesión</h5>
          <p className="text-secondary small mb-3">Accede para revisar tus contribuyentes.</p>

          <ErrorAlert message={error} onClose={() => setError(null)} />

          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label small text-body-secondary">Correo electrónico</label>
              <input
                type="email"
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="mb-4">
              <label className="form-label small text-body-secondary">Contraseña</label>
              <input
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary w-100" disabled={enviando}>
              {enviando ? 'Ingresando...' : 'Ingresar'}
            </button>
          </form>

          <p className="text-center small text-secondary mt-4 mb-0">
            ¿No tienes cuenta? <Link to="/registro">Regístrate</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
