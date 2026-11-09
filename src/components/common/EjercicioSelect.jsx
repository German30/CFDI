const ANIO_ACTUAL = new Date().getFullYear()
const ANIOS = Array.from({ length: 8 }, (_, i) => ANIO_ACTUAL - i)

export default function EjercicioSelect({ value, onChange, className = '' }) {
  return (
    <select
      className={`form-select ${className}`}
      style={{ width: 'auto' }}
      value={value}
      onChange={(e) => onChange(Number(e.target.value))}
    >
      {ANIOS.map((anio) => (
        <option key={anio} value={anio}>
          Ejercicio {anio}
        </option>
      ))}
    </select>
  )
}
