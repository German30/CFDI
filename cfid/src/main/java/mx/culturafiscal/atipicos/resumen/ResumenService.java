package mx.culturafiscal.atipicos.resumen;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.Tablero;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionService;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.motor.Contexto;
import mx.culturafiscal.atipicos.hallazgo.AnalisisEjecucion;
import mx.culturafiscal.atipicos.hallazgo.AnalisisService;
import mx.culturafiscal.atipicos.hallazgo.Hallazgo;
import mx.culturafiscal.atipicos.hallazgo.HallazgoRepositories.EjecucionRepository;
import mx.culturafiscal.atipicos.hallazgo.NivelRiesgo;
import mx.culturafiscal.atipicos.malla.MallaDtos.Fila;
import mx.culturafiscal.atipicos.malla.MallaDtos.Filtros;
import mx.culturafiscal.atipicos.malla.MallaDtos.Malla;
import mx.culturafiscal.atipicos.malla.MallaService;

/** Redacta el resumen en tercera persona y con estilo académico (apartado 8). */
@Service
@RequiredArgsConstructor
public class ResumenService {
    private static final int MAX_HALLAZGOS = 15;

    private final ResumenRepository repo;
    private final AnalisisService analisis;
    private final ConciliacionService conciliacion;
    private final MallaService malla;
    private final EjecucionRepository ejecuciones;
    private final ContribuyenteRepository contribuyentes;

    public ResumenEjecutivo obtener(Long contribuyenteId, int ejercicio) {
        return repo.findByContribuyenteIdAndEjercicio(contribuyenteId, ejercicio)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aún no se ha generado el resumen de este ejercicio"));
    }

    @Transactional
    public ResumenEjecutivo generar(Contribuyente c, int ejercicio) {
        AnalisisEjecucion e = ejecuciones.findByContribuyenteIdAndEjercicio(c.getId(), ejercicio)
                .orElseThrow(() -> new ReglaNegocioException("Debe ejecutarse el análisis antes de generar el resumen"));
        ResumenEjecutivo r = repo.findByContribuyenteIdAndEjercicio(c.getId(), ejercicio).orElseGet(ResumenEjecutivo::new);
        r.setContribuyenteId(c.getId());
        r.setEjercicio(ejercicio);
        r.setTexto(redactar(c, ejercicio, e));
        r.setEditado(false);
        r.setActualizadoEn(LocalDateTime.now());
        repo.save(r);
        contribuyentes.marcarCambio(c.getId(), LocalDateTime.now());
        return r;
    }

    public ResumenEjecutivo editar(Long contribuyenteId, int ejercicio, String texto) {
        if (texto == null || texto.isBlank()) throw new ReglaNegocioException("El texto no puede estar vacío");
        ResumenEjecutivo r = obtener(contribuyenteId, ejercicio);
        r.setTexto(texto);
        r.setEditado(true);
        r.setActualizadoEn(LocalDateTime.now());
        repo.save(r);
        contribuyentes.marcarCambio(contribuyenteId, LocalDateTime.now());
        return r;
    }

    private static String d(BigDecimal b) { return Contexto.dinero(b == null ? 0 : b.doubleValue()); }

    private BigDecimal total(Malla m, TipoComprobante t, Direccion dir) {
        return m.filas().stream().filter(f -> f.tipo() == t && f.direccion() == dir).map(Fila::totalImporte).findFirst().orElse(BigDecimal.ZERO);
    }

    private String redactar(Contribuyente c, int ejercicio, AnalisisEjecucion e) {
        Malla m = malla.construir(c, ejercicio, new Filtros(null, null, null, null, null));
        Tablero t = conciliacion.conciliar(c, ejercicio).tablero();
        List<Hallazgo> hs = analisis.listar(c.getId(), ejercicio);
        Map<NivelRiesgo, Long> n = AnalisisService.conteo(hs);
        long ops = m.filas().stream().mapToLong(Fila::totalOperaciones).sum();
        long can = m.filas().stream().mapToLong(Fila::totalCancelados).sum();
        StringBuilder s = new StringBuilder();

        s.append("# Datos del contribuyente y del ejercicio\n");
        s.append(String.format("El presente resumen corresponde a %s, con RFC %s, por el ejercicio fiscal %d.", c.getRazonSocial(), c.getRfc(), ejercicio));
        if (c.getRegimenFiscal() != null) s.append(" Su régimen fiscal registrado es ").append(c.getRegimenFiscal()).append('.');
        if (c.getActividadEconomica() != null) s.append(" La actividad económica declarada es: ").append(c.getActividadEconomica()).append('.');
        s.append("\n\n# Alcance y metodología\n");
        s.append(String.format("Se analizaron %,d CFDI del ejercicio y %,d registros de la metadata del SAT. Se aplicaron pruebas estadísticas (puntaje Z, rango intercuartílico, percentiles, ley de Benford e índice Herfindahl-Hirschman) y criterios fiscales sobre monto, frecuencia, clave de producto o servicio, nómina, traslado y cancelaciones. Cada hallazgo se ubicó en una matriz de probabilidad e impacto de 5 × 5.\n\n", e.getCfdiAnalizados(), t.registrosMetadata()));

        s.append("# Indicadores clave\n");
        s.append(String.format("- Ingresos emitidos vigentes: %s.%n", d(total(m, TipoComprobante.I, Direccion.EMITIDO))));
        s.append(String.format("- Ingresos recibidos vigentes (gastos y compras): %s.%n", d(total(m, TipoComprobante.I, Direccion.RECIBIDO))));
        s.append(String.format("- Egresos emitidos vigentes: %s.%n", d(total(m, TipoComprobante.E, Direccion.EMITIDO))));
        s.append(String.format("- Nómina emitida vigente: %s.%n", d(total(m, TipoComprobante.N, Direccion.EMITIDO))));
        s.append(String.format("- Operaciones vigentes: %,d; cancelados: %,d (%.1f%%).%n%n", ops, can, ops + can == 0 ? 0 : can * 100.0 / (ops + can)));

        s.append("# Principales hallazgos\n");
        if (hs.isEmpty()) {
            s.append("No se identificaron comportamientos atípicos con los parámetros vigentes.\n\n");
        } else {
            s.append(String.format("Se identificaron %d hallazgos: %d de nivel crítico, %d alto, %d moderado y %d bajo. A continuación se presentan los de mayor riesgo.%n", hs.size(),
                    n.get(NivelRiesgo.CRITICO), n.get(NivelRiesgo.ALTO), n.get(NivelRiesgo.MODERADO), n.get(NivelRiesgo.BAJO)));
            hs.stream().sorted(Comparator.comparing(Hallazgo::getIdentificador)).limit(MAX_HALLAZGOS).forEach(h -> s.append(String.format("- %s [%s] %s: %s%n", h.getIdentificador(), h.getNivel(), h.getCriterio(), h.getDescripcion())));
            if (hs.size() > MAX_HALLAZGOS) s.append(String.format("Los %d hallazgos restantes constan en el anexo de Excel.%n", hs.size() - MAX_HALLAZGOS));
            s.append('\n');
        }

        s.append("# Conciliación con metadata y cancelaciones\n");
        if (t.registrosMetadata() == 0) {
            s.append("Cabe advertir que no se cargó metadata del ejercicio, por lo que no fue posible confirmar el estatus de los comprobantes.\n\n");
        } else {
            s.append(String.format("Se conciliaron %,d comprobantes. Se identificaron cancelados: %,d, por %s; y CFDI vigentes en metadata sin XML cargado: %,d, por %s. Asimismo, el número de XML que no constan en la metadata fue de %,d y el de diferencias de monto, de %,d.",
                    t.conciliados(), t.cancelados(), d(t.importeCancelado()), t.faltantes(), d(t.importeFaltantes()), t.sinMetadata(), t.diferenciasMonto()));
            s.append(String.format(" Respecto de los cancelados, se identificaron los siguientes casos: fuera del ejercicio de emisión, %,d; después del plazo del art. 29-A del CFF, %,d; e ingresos con complemento de pago o egreso relacionado, %,d.%n%n",
                    t.canceladosFueraEjercicio(), t.canceladosFueraPlazo(), t.canceladosConPagoOEgreso()));
        }

        s.append("# Matriz de riesgo\n");
        s.append(String.format("La distribución de hallazgos es: crítico %d, alto %d, moderado %d y bajo %d. La probabilidad se determinó con la intensidad de la desviación y su recurrencia; el impacto, con el importe involucrado y su peso relativo en el ejercicio.%n%n",
                n.get(NivelRiesgo.CRITICO), n.get(NivelRiesgo.ALTO), n.get(NivelRiesgo.MODERADO), n.get(NivelRiesgo.BAJO)));

        s.append("# Recomendaciones\n");
        hs.stream().filter(h -> h.getNivel().compareTo(NivelRiesgo.ALTO) >= 0).map(Hallazgo::getRecomendacion).distinct().limit(8).forEach(r -> s.append("- ").append(r).append('\n'));
        s.append("- Se recomienda dar seguimiento al estatus de cada hallazgo hasta su aclaración o corrección.\n\n");

        s.append("# Limitaciones del análisis\n");
        s.append("Los resultados son indicios estadísticos y documentales; no constituyen por sí mismos una irregularidad ni una opinión legal, y requieren la revisión de un profesional. El análisis se limita a los XML y a la metadata cargados, y no se validó la vigencia de los comprobantes ante el SAT en línea. Los importes de la metadata no se convierten a moneda nacional.");
        if (e.getNoEvaluados() != null && !e.getNoEvaluados().isBlank()) {
            s.append(" No pudieron evaluarse los siguientes criterios:\n");
            for (String l : e.getNoEvaluados().split("\n")) s.append("- ").append(l).append(".\n");
        } else {
            s.append('\n');
        }
        return s.toString();
    }
}
