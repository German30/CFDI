import { client } from './client'

export const listarContribuyentes = () => client.get('/contribuyentes').then((r) => r.data)

export const obtenerContribuyente = (id) => client.get(`/contribuyentes/${id}`).then((r) => r.data)

export const crearContribuyente = (payload) => client.post('/contribuyentes', payload).then((r) => r.data)

export const editarContribuyente = (id, payload) => client.put(`/contribuyentes/${id}`, payload).then((r) => r.data)

export const eliminarContribuyente = (id) => client.delete(`/contribuyentes/${id}`)
