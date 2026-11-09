package mx.culturafiscal.atipicos.respaldo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;

@RestController
@RequestMapping("/api/contribuyentes/{id}/respaldos")
@RequiredArgsConstructor
public class RespaldoController {
    private final RespaldoService service;
    private final ContribuyenteService contribuyentes;

    public record Estado(LocalDateTime ultimoRespaldo, List<RespaldoSnapshotRepository.Info> versiones) { }

    @GetMapping
    public Estado estado(@PathVariable Long id) {
        contribuyentes.propio(id);
        return new Estado(service.ultimo(id), service.listar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@PathVariable Long id) {
        RespaldoSnapshot s = service.crearInstantanea(contribuyentes.propio(id), "MANUAL");
        return Map.of("id", s.getId(), "creadoEn", s.getCreadoEn(), "tamanoBytes", s.getTamanoBytes());
    }

    private ResponseEntity<byte[]> archivo(Contribuyente c, byte[] b) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(service.nombreArchivo(c, LocalDateTime.now())).build().toString()).body(b);
    }

    @GetMapping("/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        Contribuyente c = contribuyentes.propio(id);
        return archivo(c, service.exportarJson(c));
    }

    @GetMapping("/{respaldoId}/descarga")
    public ResponseEntity<byte[]> descargarVersion(@PathVariable Long id, @PathVariable Long respaldoId) {
        return archivo(contribuyentes.propio(id), service.jsonDeInstantanea(id, respaldoId));
    }

    @PostMapping("/restauracion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restaurar(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        try {
            service.restaurar(contribuyentes.propio(id), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostMapping("/{respaldoId}/restauracion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restaurarVersion(@PathVariable Long id, @PathVariable Long respaldoId) {
        Contribuyente c = contribuyentes.propio(id);
        service.restaurar(c, service.jsonDeInstantanea(id, respaldoId));
    }
}
