import { useEffect } from 'react'
import { X } from 'lucide-react'
import styles from './Modal.module.css'

export default function Modal({ show, title, onClose, size, children, footer }) {
  useEffect(() => {
    if (!show) return
    function handleKey(e) {
      if (e.key === 'Escape') onClose?.()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [show, onClose])

  if (!show) return null

  return (
    <>
      <div className={styles.backdrop} onClick={onClose}></div>
      <div className={styles.wrapper} role="dialog" aria-modal="true">
        <div
          className={`${styles.dialog} ${size === 'lg' ? styles.dialogLg : ''}`}
          onClick={(e) => e.stopPropagation()}
        >
          <div className={styles.header}>
            <h5 className={styles.title}>{title}</h5>
            <button type="button" className="btn-close" aria-label="Cerrar" onClick={onClose}>
              <X size={16} />
            </button>
          </div>
          <div className={styles.body}>{children}</div>
          {footer && <div className={styles.footer}>{footer}</div>}
        </div>
      </div>
    </>
  )
}
