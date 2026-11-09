import { client } from './client'

export const obtenerConciliacion = (contribuyenteId, ejercicio) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/conciliacion`, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)
