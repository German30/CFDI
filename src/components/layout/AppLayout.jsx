import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { X } from 'lucide-react'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import styles from './AppLayout.module.css'

export default function AppLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const location = useLocation()
  // Solo las primeras dos secciones de la ruta: así cambiar de pestaña dentro de
  // un contribuyente no remonta ContribuyenteLayout (y no repite su fetch).
  const sectionKey = location.pathname.split('/').filter(Boolean).slice(0, 2).join('/')

  useEffect(() => {
    if (!drawerOpen) return
    function handleKey(e) {
      if (e.key === 'Escape') setDrawerOpen(false)
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [drawerOpen])

  return (
    <div className="d-flex" style={{ minHeight: '100vh' }}>
      <aside className={styles.sidebar}>
        <Sidebar />
      </aside>

      {drawerOpen && (
        <>
          <div className={styles.drawerBackdrop} onClick={() => setDrawerOpen(false)}></div>
          <div className={styles.drawer}>
            <div className={styles.drawerClose}>
              <button className="btn-close" aria-label="Cerrar menú" onClick={() => setDrawerOpen(false)}>
                <X size={16} />
              </button>
            </div>
            <Sidebar onNavigate={() => setDrawerOpen(false)} />
          </div>
        </>
      )}

      <div className={styles.main}>
        <Topbar onMenuClick={() => setDrawerOpen(true)} />
        <main className={styles.content} key={sectionKey}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
