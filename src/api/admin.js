import { client } from './client'

export const listarUsuarios = () => client.get('/admin/usuarios').then((r) => r.data)

export const cambiarActivoUsuario = (id, activo) =>
  client.patch(`/admin/usuarios/${id}/activo`, null, { params: { activo } }).then((r) => r.data)
