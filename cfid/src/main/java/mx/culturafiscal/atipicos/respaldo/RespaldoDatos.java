package mx.culturafiscal.atipicos.respaldo;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.criterio.CriterioConfig;
import mx.culturafiscal.atipicos.hallazgo.AnalisisEjecucion;
import mx.culturafiscal.atipicos.hallazgo.Hallazgo;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;
import mx.culturafiscal.atipicos.resumen.ResumenEjecutivo;

/** Estructura versionada del archivo de respaldo. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespaldoDatos(String formato, Integer version, LocalDateTime generadoEn, Encabezado contribuyente, List<Cfdi> cfdis,
        List<MetadataRegistro> metadata, List<CriterioConfig> criterios, List<Hallazgo> hallazgos, List<AnalisisEjecucion> analisis,
        List<ResumenEjecutivo> resumenes) {

    public static final String FORMATO = "cfdi-atipicos-respaldo";
    public static final int VERSION = 1;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Encabezado(String rfc, String razonSocial, String regimenFiscal, Integer ejercicio, String actividadEconomica,
            String clavesCongruentes, String parametrosRiesgo) { }
}
