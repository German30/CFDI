package mx.culturafiscal.atipicos.datos;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** Borra en bloque los datos de un contribuyente, hijos primero, sin cargarlos en memoria. */
@Component
@RequiredArgsConstructor
public class BorradoDatos {
    private final EntityManager em;

    private static final List<String> HIJOS = List.of("CfdiConcepto", "CfdiImpuesto", "CfdiRelacionado", "CfdiPagoDocto");
    private static final List<String> TABLAS = List.of("Cfdi", "MetadataRegistro", "CriterioConfig", "Hallazgo", "AnalisisEjecucion", "ResumenEjecutivo");

    /** Usado al restaurar: conserva las instantáneas, para poder volver atrás. */
    @Transactional
    public void borrarComprobantesYAnalisis(Long id) {
        for (String h : HIJOS) {
            em.createQuery("delete from " + h + " x where x.cfdi.id in (select c.id from Cfdi c where c.contribuyenteId = :id)")
                    .setParameter("id", id).executeUpdate();
        }
        for (String t : TABLAS) {
            em.createQuery("delete from " + t + " x where x.contribuyenteId = :id").setParameter("id", id).executeUpdate();
        }
        em.clear();
    }

    /** Baja definitiva: además borra bitácoras e instantáneas. */
    @Transactional
    public void borrarDatos(Long id) {
        borrarComprobantesYAnalisis(id);
        em.createQuery("delete from ImportacionError e where e.jobId in (select j.id from ImportacionJob j where j.contribuyenteId = :id)")
                .setParameter("id", id).executeUpdate();
        em.createQuery("delete from ImportacionJob j where j.contribuyenteId = :id").setParameter("id", id).executeUpdate();
        em.createQuery("delete from RespaldoSnapshot r where r.contribuyenteId = :id").setParameter("id", id).executeUpdate();
    }
}
