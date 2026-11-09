package mx.culturafiscal.atipicos.consolidado;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.hallazgo.AnalisisService;
import mx.culturafiscal.atipicos.hallazgo.NivelRiesgo;
import mx.culturafiscal.atipicos.malla.MallaDtos.Fila;
import mx.culturafiscal.atipicos.malla.MallaDtos.Filtros;
import mx.culturafiscal.atipicos.malla.MallaDtos.Malla;
import mx.culturafiscal.atipicos.malla.MallaService;

/** Vista comparativa entre los RFC del usuario. */
@RestController
@RequestMapping("/api/consolidado")
@RequiredArgsConstructor
public class ConsolidadoController {
    private final ContribuyenteService contribuyentes;
    private final MallaService malla;
    private final AnalisisService analisis;

    public record Renglon(Long id, String rfc, String razonSocial, int ejercicio, BigDecimal ingresosEmitidos, BigDecimal ingresosRecibidos,
            BigDecimal egresosEmitidos, BigDecimal nomina, long operaciones, long cancelados, double pctCancelacion, Map<NivelRiesgo, Long> hallazgos) { }

    private static BigDecimal t(Malla m, TipoComprobante tipo, Direccion d) {
        return m.filas().stream().filter(f -> f.tipo() == tipo && f.direccion() == d).map(Fila::totalImporte).findFirst().orElse(BigDecimal.ZERO);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Renglon> consolidado(@RequestParam(required = false) Integer ejercicio) {
        List<Renglon> r = new ArrayList<>();
        for (Contribuyente c : contribuyentes.listar()) {
            int e = ejercicio != null ? ejercicio : c.getEjercicio();
            Malla m = malla.construir(c, e, new Filtros(null, null, null, null, null));
            long ops = m.filas().stream().mapToLong(Fila::totalOperaciones).sum(), can = m.filas().stream().mapToLong(Fila::totalCancelados).sum();
            r.add(new Renglon(c.getId(), c.getRfc(), c.getRazonSocial(), e, t(m, TipoComprobante.I, Direccion.EMITIDO), t(m, TipoComprobante.I, Direccion.RECIBIDO),
                    t(m, TipoComprobante.E, Direccion.EMITIDO), t(m, TipoComprobante.N, Direccion.EMITIDO), ops, can, ops + can == 0 ? 0 : can * 100.0 / (ops + can),
                    AnalisisService.conteo(analisis.listar(c.getId(), e))));
        }
        return r;
    }
}
