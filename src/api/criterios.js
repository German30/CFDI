import { client } from './client'

export const listarCriterios = (contribuyenteId) =>
  client.get(`/contribuyentes/${contribuyenteId}/criterios`).then((r) => r.data)

export const ajustarCriterio = (contribuyenteId, codigo, ajuste) =>
  client.put(`/contribuyentes/${contribuyenteId}/criterios/${codigo}`, ajuste).then((r) => r.data)
