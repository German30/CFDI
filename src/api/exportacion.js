import { client, downloadBlobResponse } from './client'

export const exportarPdf = (contribuyenteId, ejercicio) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/exportar/pdf`, {
      params: ejercicio ? { ejercicio } : {},
      responseType: 'blob',
    })
    .then((r) => downloadBlobResponse(r, `Resumen_${contribuyenteId}.pdf`))

export const exportarExcel = (contribuyenteId, ejercicio) =>
  client
    .get(`/contribuyentes/${contribuyenteId}/exportar/excel`, {
      params: ejercicio ? { ejercicio } : {},
      responseType: 'blob',
    })
    .then((r) => downloadBlobResponse(r, `Analisis_${contribuyenteId}.xlsx`))
