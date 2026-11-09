import { createContext, useContext } from 'react'

export const ContribuyenteContext = createContext(null)

export function useContribuyenteActual() {
  const ctx = useContext(ContribuyenteContext)
  if (!ctx) throw new Error('useContribuyenteActual debe usarse dentro de ContribuyenteLayout')
  return ctx
}
