import { client } from './client'

function buildFormData(archivos) {
  const formData = new FormData()
  for (const archivo of archivos) {
    formData.append('archivos', archivo)
  }
  return formData
}

export const subirXml = (contribuyenteId, archivos) =>
  client
    .post(`/contribuyentes/${contribuyenteId}/importaciones/xml`, buildFormData(archivos), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)

export const subirMetadata = (contribuyenteId, archivos) =>
  client
    .post(`/contribuyentes/${contribuyenteId}/importaciones/metadata`, buildFormData(archivos), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)

export const listarImportaciones = (contribuyenteId) =>
  client.get(`/contribuyentes/${contribuyenteId}/importaciones`).then((r) => r.data)

export const obtenerImportacion = (contribuyenteId, jobId) =>
  client.get(`/contribuyentes/${contribuyenteId}/importaciones/${jobId}`).then((r) => r.data)

export const listarErroresImportacion = (contribuyenteId, jobId, page = 0, size = 20) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/importaciones/${jobId}/errores`, {
      params: { page, size },
    })
    .then((r) => r.data)
