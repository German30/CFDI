package mx.culturafiscal.atipicos.contribuyente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteDtos.*;

@RestController
@RequestMapping("/api/contribuyentes")
@RequiredArgsConstructor
public class ContribuyenteController {
    private final ContribuyenteService service;

    @GetMapping
    public List<ContribuyenteDto> listar() { return service.listar().stream().map(ContribuyenteDto::de).toList(); }

    @GetMapping("/{id}")
    public ContribuyenteDto obtener(@PathVariable Long id) { return ContribuyenteDto.de(service.propio(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContribuyenteDto crear(@Valid @RequestBody ContribuyenteRequest r) { return ContribuyenteDto.de(service.crear(r)); }

    @PutMapping("/{id}")
    public ContribuyenteDto actualizar(@PathVariable Long id, @Valid @RequestBody ContribuyenteRequest r) {
        return ContribuyenteDto.de(service.actualizar(id, r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
