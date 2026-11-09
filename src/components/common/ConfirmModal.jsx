import { useEffect } from 'react'
import styles from './Modal.module.css'

export default function ConfirmModal({
  show,
  title,
  children,
  confirmLabel = 'Confirmar',
  confirmVariant = 'danger',
  onConfirm,
  onCancel,
  confirmDisabled = false,
}) {
  useEffect(() => {
    if (!show) return
    function handleKey(e) {
      if (e.key === 'Escape') onCancel?.()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [show, onCancel])

  if (!show) return null

  return (
    <>
      <div className={styles.backdrop} onClick={onCancel}></div>
      <div className={styles.wrapper} role="dialog" aria-modal="true">
        <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
          <div className={styles.header}>
            <h5 className={styles.title}>{title}</h5>
          </div>
          <div className={styles.body}>{children}</div>
          <div className={styles.footer}>
            <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
              Cancelar
            </button>
            <button
              type="button"
              className={`btn btn-${confirmVariant === 'danger' ? 'danger' : confirmVariant === 'warning' ? 'outline-warning' : 'primary'}`}
              onClick={onConfirm}
              disabled={confirmDisabled}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
