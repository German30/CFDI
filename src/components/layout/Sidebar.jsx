import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Users, OctagonAlert, ShieldAlert, Trash2, ShieldCheck } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

const LINKS = [
  { to: '/', label: 'Panel general', Icon: LayoutDashboard, end: true },
  { to: '/contribuyentes', label: 'Contribuyentes', Icon: Users },
  { to: '/lista-69b', label: 'Lista 69-B', Icon: OctagonAlert },
]

const ADMIN_LINKS = [{ to: '/admin/usuarios', label: 'Usuarios', Icon: ShieldAlert }]

export default function Sidebar({ onNavigate }) {
  const { isAdmin } = useAuth()
  const links = isAdmin ? [...LINKS, ...ADMIN_LINKS] : LINKS

  return (
    <div className="d-flex flex-column h-100 py-3">
      <div className="d-flex align-items-center gap-2 px-3 mb-4">
        <span
          className="chip bg-primary text-white fw-semibold"
          style={{ width: 30, height: 30, borderRadius: 8 }}
        >
          <ShieldCheck size={16} />
        </span>
        <div className="lh-sm">
          <div className="fw-semibold" style={{ fontSize: 14 }}>
            CFDI Atípicos
          </div>
          <div className="text-secondary" style={{ fontSize: 11.5 }}>
            Detector de riesgo fiscal
          </div>
        </div>
      </div>

      <nav className="flex-grow-1 px-2">
        <ul className="nav nav-pills flex-column gap-1">
          {links.map((link) => (
            <li className="nav-item" key={link.to}>
              <NavLink
                to={link.to}
                end={link.end}
                onClick={onNavigate}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                style={{ fontSize: 14, width: '100%' }}
              >
                <link.Icon size={16} />
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="px-2 pt-2 border-top mx-2">
        <NavLink
          to="/ajustes/eliminacion"
          onClick={onNavigate}
          className={({ isActive }) => `nav-link nav-link-danger mt-2 ${isActive ? 'active' : ''}`}
          style={{ fontSize: 14, width: '100%' }}
        >
          <Trash2 size={16} />
          Zona de riesgo
        </NavLink>
      </div>
    </div>
  )
}
