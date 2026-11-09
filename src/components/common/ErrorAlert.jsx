export default function ErrorAlert({ message, onClose }) {
  if (!message) return null
  return (
    <div className="alert alert-danger d-flex align-items-start justify-content-between" role="alert">
      <span>{message}</span>
      {onClose && (
        <button type="button" className="btn-close" aria-label="Cerrar" onClick={onClose}></button>
      )}
    </div>
  )
}
