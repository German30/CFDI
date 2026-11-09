import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Menu, ChevronDown, LogOut } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

function iniciales(nombre) {
  if (!nombre) return '?'
  const partes = nombre.trim().split(/\s+/)
  return (partes[0]?.[0] || '').concat(partes[1]?.[0] || '').toUpperCase()
}

export default function Topbar({ onMenuClick }) {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef(null)

  useEffect(() => {
    if (!menuOpen) return
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header
      className="d-flex align-items-center justify-content-between bg-white border-bottom px-3 px-md-4"
      style={{ height: 56 }}
    >
      <button className="btn btn-sm btn-outline-secondary d-lg-none" type="button" onClick={onMenuClick}>
        <Menu size={16} />
      </button>
      <span className="d-lg-block"></span>

      <div className="dropdown" ref={menuRef}>
        <button
          className="btn d-flex align-items-center gap-2 border-0 bg-transparent"
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
        >
          <span
            className="chip bg-primary-subtle text-primary-emphasis fw-semibold"
            style={{ width: 30, height: 30, fontSize: 12, borderRadius: '50%' }}
          >
            {iniciales(usuario?.nombre)}
          </span>
          <span className="d-sm-inline text-secondary" style={{ fontSize: 14 }}>
            {usuario?.nombre}
          </span>
          <ChevronDown size={14} className="text-secondary" />
        </button>
        {menuOpen && (
          <ul className="dropdown-menu dropdown-menu-end">
            <li>
              <span className="dropdown-item-text small text-secondary">{usuario?.email}</span>
            </li>
            <li>
              <hr className="dropdown-divider" />
            </li>
            <li>
              <button className="dropdown-item text-danger" onClick={handleLogout}>
                <LogOut size={14} />
                Cerrar sesión
              </button>
            </li>
          </ul>
        )}
      </div>
    </header>
  )
}
