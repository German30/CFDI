package mx.culturafiscal.atipicos.hallazgo;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.*;

@RestController
@RequestMapping("/api/contribuyentes/{id}")
@RequiredArgsConstructor
public class HallazgoController {
    private final AnalisisService service;
    private final ContribuyenteService contribuyentes;

    private int ej(Contribuyente c, Integer e) { return e != null ? e : c.getEjercicio(); }

    @PostMapping("/analisis")
    public ResultadoAnalisis analizar(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.analizar(c, ej(c, ejercicio));
    }

    @GetMapping("/hallazgos")
    public List<HallazgoDto> hallazgos(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio,
            @RequestParam(required = false) NivelRiesgo nivel, @RequestParam(required = false) EstatusHallazgo estatus,
            @RequestParam(required = false) String criterio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.listar(id, ej(c, ejercicio)).stream()
                .filter(h -> nivel == null || h.getNivel() == nivel)
                .filter(h -> estatus == null || h.getEstatus() == estatus)
                .filter(h -> criterio == null || criterio.equals(h.getCodigoCriterio()))
                .map(HallazgoDto::de).toList();
    }

    @GetMapping("/hallazgos/{hallazgoId}")
    public HallazgoDetalle detalle(@PathVariable Long id, @PathVariable Long hallazgoId) {
        contribuyentes.propio(id);
        Hallazgo h = service.obtener(id, hallazgoId);
        List<String> u = h.getUuids() == null || h.getUuids().isEmpty() ? List.of() : Arrays.asList(h.getUuids().split(","));
        return new HallazgoDetalle(HallazgoDto.de(h), u);
    }

    @PatchMapping("/hallazgos/{hallazgoId}")
    public HallazgoDto estatus(@PathVariable Long id, @PathVariable Long hallazgoId, @Valid @RequestBody CambioEstatus r) {
        contribuyentes.propio(id);
        return HallazgoDto.de(service.cambiarEstatus(id, hallazgoId, r));
    }

    @GetMapping("/matriz")
    public Matriz matriz(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.matriz(c, ej(c, ejercicio));
    }

    @GetMapping("/parametros-riesgo")
    public ParametrosRiesgo parametros(@PathVariable Long id) { return service.parametros(contribuyentes.propio(id)); }

    @PutMapping("/parametros-riesgo")
    public ParametrosRiesgo guardar(@PathVariable Long id, @Valid @RequestBody ParametrosRiesgo p) {
        return service.guardarParametros(contribuyentes.propio(id), p);
    }
}
