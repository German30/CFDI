import { client, downloadBlobResponse } from './client'

export const obtenerEstadoRespaldos = (contribuyenteId) =>
  client.get(`/contribuyentes/${contribuyenteId}/respaldos`).then((r) => r.data)

export const crearRespaldo = (contribuyenteId) =>
  client.post(`/contribuyentes/${contribuyenteId}/respaldos`).then((r) => r.data)

export const descargarRespaldoActual = (contribuyenteId) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/respaldos/descarga`, { responseType: 'blob' })
    .then((r) => downloadBlobResponse(r, `respaldo_${contribuyenteId}.json`))

export const descargarRespaldoVersion = (contribuyenteId, respaldoId) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/respaldos/${respaldoId}/descarga`, { responseType: 'blob' })
    .then((r) => downloadBlobResponse(r, `respaldo_${contribuyenteId}_${respaldoId}.json`))

export const restaurarDesdeArchivo = (contribuyenteId, archivo) => {
  const formData = new FormData()
  formData.append('archivo', archivo)
  return client.post(`/contribuyentes/${contribuyenteId}/respaldos/restauracion`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const restaurarDesdeVersion = (contribuyenteId, respaldoId) =>
  client.post(`/contribuyentes/${contribuyenteId}/respaldos/${respaldoId}/restauracion`)
