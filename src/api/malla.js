import { client } from './client'

export const obtenerMalla = (contribuyenteId, filtros = {}) =>
  client.get(`/contribuyentes/${contribuyenteId}/malla`, { params: filtros }).then((r) => r.data)

export const obtenerMallaDetalle = (contribuyenteId, filtros = {}) =>
  client.get(`/contribuyentes/${contribuyenteId}/malla/detalle`, { params: filtros }).then((r) => r.data)
