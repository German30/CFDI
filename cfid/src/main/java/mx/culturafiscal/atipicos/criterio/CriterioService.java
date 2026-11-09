package mx.culturafiscal.atipicos.criterio;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.motor.CatalogoCriterios;
import mx.culturafiscal.atipicos.criterio.motor.Criterio;

@Service
@RequiredArgsConstructor
public class CriterioService {
    private final CatalogoCriterios catalogo;
    private final CriterioConfigRepository repo;
    private final ContribuyenteRepository contribuyentes;
    private final ObjectMapper json;

    public record Efectivo(boolean activo, Map<String, Double> parametros) { }

    public record Vista(String codigo, String categoria, String nombre, String fundamento, String consecuencia, String recomendacion,
            boolean activo, Map<String, Double> parametros, Map<String, Double> parametrosPorDefecto) { }

    public record Ajuste(Boolean activo, Map<String, Double> parametros) { }

    public Map<String, Efectivo> efectivos(Long contribuyenteId) {
        Map<String, CriterioConfig> cfg = new HashMap<>();
        repo.findByContribuyenteId(contribuyenteId).forEach(c -> cfg.put(c.getCodigo(), c));
        Map<String, Efectivo> r = new LinkedHashMap<>();
        for (Criterio c : catalogo.todos()) {
            Map<String, Double> p = new LinkedHashMap<>(c.parametros());
            CriterioConfig k = cfg.get(c.codigo());
            if (k != null && k.getParametros() != null) p.putAll(leer(k.getParametros()));
            r.put(c.codigo(), new Efectivo(k == null || k.isActivo(), p));
        }
        return r;
    }

    public List<Vista> vistas(Long contribuyenteId) {
        Map<String, Efectivo> e = efectivos(contribuyenteId);
        return catalogo.todos().stream().map(c -> new Vista(c.codigo(), c.categoria(), c.nombre(), c.fundamento(), c.consecuencia(),
                c.recomendacion(), e.get(c.codigo()).activo(), e.get(c.codigo()).parametros(), c.parametros())).toList();
    }

    @Transactional
    public void ajustar(Long contribuyenteId, String codigo, Ajuste a) {
        Criterio c = catalogo.buscar(codigo).orElseThrow(() -> new RecursoNoEncontradoException("Criterio no encontrado: " + codigo));
        CriterioConfig k = repo.findByContribuyenteIdAndCodigo(contribuyenteId, codigo).orElseGet(() -> {
            CriterioConfig n = new CriterioConfig();
            n.setContribuyenteId(contribuyenteId);
            n.setCodigo(codigo);
            return n;
        });
        if (a.activo() != null) k.setActivo(a.activo());
        if (a.parametros() != null) {
            for (var e : a.parametros().entrySet()) {
                if (!c.parametros().containsKey(e.getKey())) throw new ReglaNegocioException("Parámetro desconocido: " + e.getKey());
                if (e.getValue() == null || e.getValue() < 0) throw new ReglaNegocioException("Valor inválido para " + e.getKey());
            }
            try {
                k.setParametros(json.writeValueAsString(a.parametros()));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
        repo.save(k);
        contribuyentes.marcarCambio(contribuyenteId, LocalDateTime.now());
    }

    private Map<String, Double> leer(String s) {
        try {
            return json.readValue(s, new TypeReference<Map<String, Double>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Parámetros corruptos", e);
        }
    }
}
