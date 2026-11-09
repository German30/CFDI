import { useEffect, useState } from 'react'
import Modal from '../../components/common/Modal'
import ErrorAlert from '../../components/common/ErrorAlert'
import { apiErrorMessage } from '../../api/client'
import { OPCIONES_REGIMEN_FISCAL } from '../../data/regimenesFiscales'

const VACIO = {
  rfc: '',
  razonSocial: '',
  regimenFiscal: '',
  ejercicio: new Date().getFullYear(),
  actividadEconomica: '',
  clavesCongruentes: '',
}

export default function ContribuyenteFormModal({ show, contribuyente, onClose, onSubmit }) {
  const [form, setForm] = useState(VACIO)
  const [error, setError] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [catalogoActividades, setCatalogoActividades] = useState(null)

  useEffect(() => {
    if (show) {
      setForm(contribuyente ? { ...VACIO, ...contribuyente } : VACIO)
      setError(null)
    }
  }, [show, contribuyente])

  useEffect(() => {
    if (show && !catalogoActividades) {
      import('../../data/actividadesEconomicas').then(setCatalogoActividades)
    }
  }, [show, catalogoActividades])

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      await onSubmit(form)
      onClose()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Modal show={show} onClose={onClose} title={contribuyente ? 'Editar contribuyente' : 'Nuevo contribuyente'}>
      <ErrorAlert message={error} onClose={() => setError(null)} />
      <form id="contribuyente-form" onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">RFC</label>
          <input
            type="text"
            className="form-control text-uppercase"
            value={form.rfc}
            onChange={(e) => set('rfc', e.target.value.toUpperCase())}
            disabled={!!contribuyente}
            required
            maxLength={13}
          />
        </div>
        <div className="mb-3">
          <label className="form-label">Razón social</label>
          <input
            type="text"
            className="form-control"
            value={form.razonSocial}
            onChange={(e) => set('razonSocial', e.target.value)}
            required
          />
        </div>
        <div className="row">
          <div className="col-6 mb-3">
            <label className="form-label">Ejercicio</label>
            <input
              type="number"
              className="form-control"
              value={form.ejercicio}
              onChange={(e) => set('ejercicio', Number(e.target.value))}
              min={2014}
              max={2100}
              required
            />
          </div>
          <div className="col-6 mb-3">
            <CampoConCatalogo
              label="Régimen fiscal"
              value={form.regimenFiscal}
              opciones={OPCIONES_REGIMEN_FISCAL}
              onChange={(v) => set('regimenFiscal', v)}
            />
          </div>
        </div>
        <div className="mb-3">
          {catalogoActividades ? (
            <CampoConCatalogo
              label="Actividad económica"
              value={form.actividadEconomica}
              opciones={catalogoActividades.OPCIONES_ACTIVIDAD_ECONOMICA}
              grupos={catalogoActividades.ACTIVIDADES_POR_GRUPO}
              onChange={(v) => set('actividadEconomica', v)}
            />
          ) : (
            <div>
              <label className="form-label">Actividad económica</label>
              <select className="form-select" disabled>
                <option>Cargando catálogo del SAT...</option>
              </select>
            </div>
          )}
        </div>
        <div className="mb-1">
          <label className="form-label">Claves de producto/servicio congruentes</label>
          <input
            type="text"
            className="form-control"
            placeholder="Ej. 8411, 4312, 7211"
            value={form.clavesCongruentes || ''}
            onChange={(e) => set('clavesCongruentes', e.target.value)}
          />
          <div className="form-text">Prefijos de ClaveProdServ separados por coma.</div>
        </div>
      </form>
      <div className="d-flex justify-content-end gap-2 mt-3">
        <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
          Cancelar
        </button>
        <button type="submit" form="contribuyente-form" className="btn btn-primary" disabled={guardando}>
          {guardando ? 'Guardando...' : 'Guardar'}
        </button>
      </div>
    </Modal>
  )
}

// opciones: lista plana (régimen fiscal). grupos (opcional): [{ grupo, actividades: [{clave, descripcion}] }]
// para renderizar <optgroup> en catálogos grandes (actividad económica).
function CampoConCatalogo({ label, value, opciones, grupos, onChange }) {
  const valorActual = value || ''

  return (
    <div>
      <label className="form-label">{label}</label>
      <select className="form-select" value={valorActual} onChange={(e) => onChange(e.target.value)}>
        <option value="">Selecciona...</option>
        {grupos
          ? grupos.map(({ grupo, actividades }) => (
              <optgroup key={grupo} label={grupo}>
                {actividades.map((a) => {
                  const o = `${a.clave} - ${a.descripcion}`
                  return (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  )
                })}
              </optgroup>
            ))
          : opciones.map((o) => (
              <option key={o} value={o}>
                {o}
              </option>
            ))}
      </select>
    </div>
  )
}
