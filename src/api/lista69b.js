import { client } from './client'

export const importarLista69b = (archivo) => {
  const formData = new FormData()
  formData.append('archivo', archivo)
  return client
    .post('/lista-69b', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

export const contarLista69b = () => client.get('/lista-69b').then((r) => r.data)
