import catalogo from './actividadesEconomicas.json'

// Catálogo "Actividades Económicas" — Anexo 6 de la Resolución Miscelánea Fiscal
// (SAT, compilado a la Octava Modificación 2024). Fuente:
// https://www.sat.gob.mx/minisitio/NormatividadRMFyRGCE/documentos2025/rmf/compiladas/Compilado_Octava_Modificacion_Anexo_6_RMF2014.pdf
// Extraído automáticamente del PDF oficial (154 páginas, 1,330 claves). La gran
// mayoría de las descripciones son exactas; una minoría puede verse truncada o
// con texto repetido por las limitaciones de extraer texto de una tabla en PDF.
// Si encuentras una clave incorrecta, usa "+ Agregar nuevo..." en el formulario.

const GRUPOS = []
const porGrupo = new Map()
for (const entry of catalogo) {
  const grupo = entry.grupo || 'Otras actividades'
  if (!porGrupo.has(grupo)) {
    porGrupo.set(grupo, [])
    GRUPOS.push(grupo)
  }
  porGrupo.get(grupo).push(entry)
}

export const ACTIVIDADES_POR_GRUPO = GRUPOS.map((grupo) => ({
  grupo,
  actividades: porGrupo.get(grupo),
}))

export const OPCIONES_ACTIVIDAD_ECONOMICA = catalogo.map((e) => `${e.clave} - ${e.descripcion}`)
