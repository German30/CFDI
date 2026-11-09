export default function LoadingSpinner({ label = 'Cargando...' }) {
  return (
    <div className="d-flex align-items-center justify-content-center gap-2 text-secondary py-5">
      <div className="spinner spinner-sm" role="status" aria-hidden="true"></div>
      <span>{label}</span>
    </div>
  )
}
