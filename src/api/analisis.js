import { client } from './client'

export const ejecutarAnalisis = (contribuyenteId, ejercicio) =>
  client
    .post(`/contribuyentes/${contribuyenteId}/analisis`, null, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)

export const listarHallazgos = (contribuyenteId, filtros = {}) =>
  client.get(`/contribuyentes/${contribuyenteId}/hallazgos`, { params: filtros }).then((r) => r.data)

export const obtenerHallazgo = (contribuyenteId, hallazgoId) =>
  client.get(`/contribuyentes/${contribuyenteId}/hallazgos/${hallazgoId}`).then((r) => r.data)

export const actualizarHallazgo = (contribuyenteId, hallazgoId, payload) =>
  client.patch(`/contribuyentes/${contribuyenteId}/hallazgos/${hallazgoId}`, payload).then((r) => r.data)

export const obtenerMatriz = (contribuyenteId, ejercicio) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/matriz`, { params: ejercicio ? { ejercicio } : {} })
    .then((r) => r.data)

export const obtenerParametrosRiesgo = (contribuyenteId) =>
  client.get(`/contribuyentes/${contribuyenteId}/parametros-riesgo`).then((r) => r.data)

export const actualizarParametrosRiesgo = (contribuyenteId, payload) =>
  client.put(`/contribuyentes/${contribuyenteId}/parametros-riesgo`, payload).then((r) => r.data)
