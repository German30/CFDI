import { createContext, useContext, useEffect, useState } from 'react'
import * as authApi from '../api/auth'
import { clearTokens, getTokens, setTokens } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => {
    const stored = localStorage.getItem('usuario')
    return stored ? JSON.parse(stored) : null
  })
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    const { accessToken } = getTokens()
    if (!accessToken) {
      setCargando(false)
      return
    }
    authApi
      .me()
      .then((data) => {
        setUsuario(data)
        localStorage.setItem('usuario', JSON.stringify(data))
      })
      .catch(() => {
        clearTokens()
        setUsuario(null)
      })
      .finally(() => setCargando(false))
  }, [])

  async function login(email, password) {
    const data = await authApi.login(email, password)
    setTokens(data)
    localStorage.setItem('usuario', JSON.stringify(data.usuario))
    setUsuario(data.usuario)
    return data.usuario
  }

  async function registro(nombre, email, password) {
    const data = await authApi.registro(nombre, email, password)
    setTokens(data)
    localStorage.setItem('usuario', JSON.stringify(data.usuario))
    setUsuario(data.usuario)
    return data.usuario
  }

  function logout() {
    clearTokens()
    setUsuario(null)
  }

  const value = {
    usuario,
    cargando,
    isAuthenticated: !!usuario,
    isAdmin: usuario?.rol === 'ADMIN',
    login,
    registro,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
