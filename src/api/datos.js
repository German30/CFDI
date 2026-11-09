import { client } from './client'

export const solicitarEliminacionTotal = () => client.post('/datos/eliminacion').then((r) => r.data)

export const confirmarEliminacionTotal = (token) =>
  client.delete('/datos/eliminacion', { data: { token, frase: 'ELIMINAR TODO' } })
