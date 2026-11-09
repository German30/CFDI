import { client } from './client'

export const obtenerResumen = (contribuyenteId, ejercicio) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/resumen`, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)

export const generarResumen = (contribuyenteId, ejercicio) =>
  client
    .post(`/contribuyentes/${contribuyenteId}/resumen`, null, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)

export const editarResumen = (contribuyenteId, ejercicio, texto) =>
  client
    .put(`/contribuyentes/${contribuyenteId}/resumen`, { texto }, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)
