package mx.culturafiscal.atipicos.hallazgo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Cfdi;
import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.Lista69bRepository;
import mx.culturafiscal.atipicos.criterio.motor.CatalogoCriterios;
import mx.culturafiscal.atipicos.criterio.motor.Contexto;
import mx.culturafiscal.atipicos.criterio.motor.Contexto.Borrador;
import mx.culturafiscal.atipicos.criterio.motor.Criterio;
import mx.culturafiscal.atipicos.criterio.motor.Motor;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.*;
import mx.culturafiscal.atipicos.hallazgo.HallazgoRepositories.EjecucionRepository;
import mx.culturafiscal.atipicos.hallazgo.HallazgoRepositories.HallazgoRepository;
import mx.culturafiscal.atipicos.metadata.MetadataRegistro;
import mx.culturafiscal.atipicos.metadata.MetadataRegistroRepository;

@Service
@RequiredArgsConstructor
public class AnalisisService {
    private final CfdiRepository cfdis;
    private final MetadataRegistroRepository metadata;
    private final Lista69bRepository lista69b;
    private final HallazgoRepository hallazgos;
    private final EjecucionRepository ejecuciones;
    private final ContribuyenteRepository contribuyentes;
    private final CatalogoCriterios catalogo;
    private final Motor motor;
    private final ObjectMapper json;

    public ParametrosRiesgo parametros(Contribuyente c) {
        if (c.getParametrosRiesgo() == null) return ParametrosRiesgo.porDefecto();
        try {
            return json.readValue(c.getParametrosRiesgo(), ParametrosRiesgo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Parámetros de riesgo corruptos", e);
        }
    }

    public ParametrosRiesgo guardarParametros(Contribuyente c, ParametrosRiesgo p) {
        try {
            c.setParametrosRiesgo(json.writeValueAsString(p));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        c.setModificadoEn(LocalDateTime.now());
        contribuyentes.save(c);
        return p;
    }

    @Transactional
    public ResultadoAnalisis analizar(Contribuyente c, int ejercicio) {
        List<Cfdi> todos = cfdis.findByContribuyenteId(c.getId());
        Set<String> cargados = new HashSet<>();
        todos.forEach(x -> cargados.add(x.getUuid()));
        List<MetadataRegistro> sinXml = metadata.findByContribuyenteId(c.getId()).stream()
                .filter(m -> m.isCancelado() && !cargados.contains(m.getUuid()) && m.getFechaEmision() != null && m.getFechaEmision().getYear() == ejercicio).toList();
        Map<String, String> l69 = new HashMap<>();
        lista69b.findByUsuarioId(c.getUsuarioId()).forEach(l -> l69.put(l.getRfc(), l.getSituacion() == null ? "" : l.getSituacion()));

        Contexto x = new Contexto(c, ejercicio, todos, sinXml, l69);
        motor.ejecutar(x);

        double total = x.vigentes.stream().filter(v -> v.getTipo() == TipoComprobante.I || v.getTipo() == TipoComprobante.N)
                .mapToDouble(v -> Contexto.n(v.getImporteMxn())).sum();
        ParametrosRiesgo p = parametros(c);
        Map<String, Long> recurrencia = new HashMap<>();
        x.salida.forEach(b -> recurrencia.merge(b.codigo(), 1L, Long::sum));

        Map<String, Hallazgo> previos = new HashMap<>();
        hallazgos.findByContribuyenteIdAndEjercicioOrderByIdentificador(c.getId(), ejercicio).forEach(h -> previos.put(h.getHuella(), h));
        Map<String, EstatusHallazgo> estatus = new HashMap<>();
        Map<String, String> notas = new HashMap<>();
        previos.forEach((k, h) -> { estatus.put(k, h.getEstatus()); notas.put(k, h.getNotas()); });
        hallazgos.borrar(c.getId(), ejercicio);

        List<Hallazgo> nuevos = new ArrayList<>();
        for (Borrador b : x.salida) {
            Criterio cr = catalogo.buscar(b.codigo()).orElseThrow();
            var r = CalculadoraRiesgo.calcular(b.intensidad(), recurrencia.get(b.codigo()).intValue(), Math.abs(b.importe()), total, p);
            Hallazgo h = new Hallazgo();
            h.setContribuyenteId(c.getId());
            h.setEjercicio(ejercicio);
            h.setHuella(UUID.nameUUIDFromBytes((b.codigo() + "|" + b.clave() + "|" + b.periodo()).getBytes(StandardCharsets.UTF_8)).toString());
            h.setCodigoCriterio(b.codigo());
            h.setCriterio(cr.nombre());
            h.setDescripcion(b.descripcion().length() > 1500 ? b.descripcion().substring(0, 1500) : b.descripcion());
            h.setPeriodo(b.periodo());
            h.setNumCfdi(b.uuids().size());
            h.setUuids(String.join(",", b.uuids()));
            h.setImporte(BigDecimal.valueOf(b.importe()).setScale(2, RoundingMode.HALF_UP));
            h.setIntensidad(b.intensidad());
            h.setRecurrencia(recurrencia.get(b.codigo()).intValue());
            h.setProbabilidad(r.probabilidad());
            h.setImpacto(r.impacto());
            h.setNivel(r.nivel());
            h.setConsecuencia(cr.consecuencia());
            h.setRecomendacion(cr.recomendacion());
            h.setEstatus(estatus.getOrDefault(h.getHuella(), EstatusHallazgo.PENDIENTE));
            h.setNotas(notas.get(h.getHuella()));
            nuevos.add(h);
        }
        nuevos.sort(Comparator.comparing(Hallazgo::getNivel).reversed()
                .thenComparing(Comparator.comparingInt((Hallazgo h) -> h.getProbabilidad() * h.getImpacto()).reversed())
                .thenComparing(Hallazgo::getImporte, Comparator.reverseOrder()));
        for (int i = 0; i < nuevos.size(); i++) nuevos.get(i).setIdentificador(String.format("H-%d-%04d", ejercicio, i + 1));
        hallazgos.saveAll(nuevos);

        AnalisisEjecucion e = ejecuciones.findByContribuyenteIdAndEjercicio(c.getId(), ejercicio).orElseGet(AnalisisEjecucion::new);
        e.setContribuyenteId(c.getId());
        e.setEjercicio(ejercicio);
        e.setFecha(LocalDateTime.now());
        e.setCfdiAnalizados(x.anio.size());
        e.setNoEvaluados(String.join("\n", x.noEvaluados));
        ejecuciones.save(e);
        contribuyentes.marcarCambio(c.getId(), LocalDateTime.now());
        return new ResultadoAnalisis(ejercicio, e.getFecha(), x.anio.size(), nuevos.size(), conteo(nuevos), x.noEvaluados);
    }

    public static Map<NivelRiesgo, Long> conteo(List<Hallazgo> l) {
        Map<NivelRiesgo, Long> r = new EnumMap<>(NivelRiesgo.class);
        for (NivelRiesgo n : NivelRiesgo.values()) r.put(n, 0L);
        l.forEach(h -> r.merge(h.getNivel(), 1L, Long::sum));
        return r;
    }

    public List<Hallazgo> listar(Long contribuyenteId, int ejercicio) {
        return hallazgos.findByContribuyenteIdAndEjercicioOrderByIdentificador(contribuyenteId, ejercicio);
    }

    public Hallazgo obtener(Long contribuyenteId, Long id) {
        return hallazgos.findByIdAndContribuyenteId(id, contribuyenteId).orElseThrow(() -> new RecursoNoEncontradoException("Hallazgo no encontrado"));
    }

    public Hallazgo cambiarEstatus(Long contribuyenteId, Long id, CambioEstatus r) {
        Hallazgo h = obtener(contribuyenteId, id);
        h.setEstatus(r.estatus());
        if (r.notas() != null) h.setNotas(r.notas());
        hallazgos.save(h);
        contribuyentes.marcarCambio(contribuyenteId, LocalDateTime.now());
        return h;
    }

    public Matriz matriz(Contribuyente c, int ejercicio) {
        List<Hallazgo> l = listar(c.getId(), ejercicio);
        ParametrosRiesgo p = parametros(c);
        List<CeldaMatriz> celdas = new ArrayList<>();
        for (int prob = 5; prob >= 1; prob--) {
            for (int imp = 1; imp <= 5; imp++) {
                final int a = prob, b = imp;
                List<Hallazgo> g = l.stream().filter(h -> h.getProbabilidad() == a && h.getImpacto() == b).toList();
                celdas.add(new CeldaMatriz(prob, imp, CalculadoraRiesgo.nivel(prob, imp, p), g.size(),
                        g.stream().map(Hallazgo::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add), g.stream().map(Hallazgo::getIdentificador).toList()));
            }
        }
        return new Matriz(ejercicio, celdas, conteo(l));
    }
}
