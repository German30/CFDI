import { client } from './client'

export const obtenerConsolidado = (ejercicio) =>
  client.get('/consolidado', { params: ejercicio ? { ejercicio } : {} }).then((r) => r.data)
