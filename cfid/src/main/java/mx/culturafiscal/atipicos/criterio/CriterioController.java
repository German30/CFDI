package mx.culturafiscal.atipicos.criterio;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.criterio.CriterioService.Ajuste;
import mx.culturafiscal.atipicos.criterio.CriterioService.Vista;
import mx.culturafiscal.atipicos.security.SecurityUtils;

@RestController
@RequiredArgsConstructor
public class CriterioController {
    private final CriterioService service;
    private final ContribuyenteService contribuyentes;
    private final Lista69bService lista;
    private final Lista69bRepository listaRepo;

    @GetMapping("/api/contribuyentes/{id}/criterios")
    public List<Vista> criterios(@PathVariable Long id) {
        contribuyentes.propio(id);
        return service.vistas(id);
    }

    @PutMapping("/api/contribuyentes/{id}/criterios/{codigo}")
    public List<Vista> ajustar(@PathVariable Long id, @PathVariable String codigo, @RequestBody Ajuste a) {
        contribuyentes.propio(id);
        service.ajustar(id, codigo, a);
        return service.vistas(id);
    }

    @PostMapping("/api/lista-69b")
    public Map<String, Integer> importar(@RequestParam("archivo") MultipartFile archivo) {
        return Map.of("registros", lista.importar(archivo));
    }

    @GetMapping("/api/lista-69b")
    public Map<String, Long> resumen() {
        return Map.of("registros", listaRepo.countByUsuarioId(SecurityUtils.usuarioActual().getId()));
    }
}
