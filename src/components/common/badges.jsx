const NIVEL_CLASES = {
  BAJO: 'bg-success-subtle text-success-emphasis',
  MODERADO: 'bg-warning-subtle text-warning-emphasis',
  ALTO: 'bg-orange-subtle text-orange-emphasis',
  CRITICO: 'bg-danger-subtle text-danger-emphasis',
}

const ESTATUS_CLASES = {
  PENDIENTE: 'bg-secondary-subtle text-secondary-emphasis',
  EN_REVISION: 'bg-info-subtle text-info-emphasis',
  ACLARADO: 'bg-primary-subtle text-primary-emphasis',
  CORREGIDO: 'bg-success-subtle text-success-emphasis',
}

const ESTADO_IMPORTACION_CLASES = {
  EN_COLA: 'bg-secondary-subtle text-secondary-emphasis',
  PROCESANDO: 'bg-info-subtle text-info-emphasis',
  COMPLETADO: 'bg-success-subtle text-success-emphasis',
  FALLIDO: 'bg-danger-subtle text-danger-emphasis',
}

export function NivelBadge({ nivel }) {
  if (!nivel) return null
  return <span className={`badge rounded-pill ${NIVEL_CLASES[nivel] || 'bg-secondary-subtle text-secondary-emphasis'}`}>{nivel}</span>
}

export function EstatusBadge({ estatus }) {
  if (!estatus) return null
  return (
    <span className={`badge rounded-pill ${ESTATUS_CLASES[estatus] || 'bg-secondary-subtle text-secondary-emphasis'}`}>
      {estatus.replace('_', ' ')}
    </span>
  )
}

export function EstadoImportacionBadge({ estado }) {
  if (!estado) return null
  return (
    <span className={`badge rounded-pill ${ESTADO_IMPORTACION_CLASES[estado] || 'bg-secondary-subtle text-secondary-emphasis'}`}>
      {estado}
    </span>
  )
}
