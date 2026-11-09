package mx.culturafiscal.atipicos.conciliacion;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.Resultado;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;

@RestController
@RequestMapping("/api/contribuyentes/{id}/conciliacion")
@RequiredArgsConstructor
public class ConciliacionController {
    private final ConciliacionService service;
    private final ContribuyenteService contribuyentes;

    @GetMapping
    public Resultado conciliar(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        return service.conciliar(c, ejercicio != null ? ejercicio : c.getEjercicio());
    }
}
