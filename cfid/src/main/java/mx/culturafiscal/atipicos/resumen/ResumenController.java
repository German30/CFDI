package mx.culturafiscal.atipicos.resumen;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;

@RestController
@RequestMapping("/api/contribuyentes/{id}/resumen")
@RequiredArgsConstructor
public class ResumenController {
    private final ResumenService service;
    private final ContribuyenteService contribuyentes;

    public record Edicion(String texto) { }

    private int ej(Contribuyente c, Integer e) { return e != null ? e : c.getEjercicio(); }

    @GetMapping
    public ResumenEjecutivo obtener(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.obtener(id, ej(c, ejercicio));
    }

    @PostMapping
    public ResumenEjecutivo generar(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.generar(c, ej(c, ejercicio));
    }

    @PutMapping
    public ResumenEjecutivo editar(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio, @RequestBody Edicion e) {
        Contribuyente c = contribuyentes.propio(id);
        return service.editar(id, ej(c, ejercicio), e.texto());
    }
}
