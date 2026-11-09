package mx.culturafiscal.atipicos.importacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contribuyentes/{id}/importaciones")
@RequiredArgsConstructor
public class ImportacionController {
    private final ImportacionService service;

    @PostMapping("/xml")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportacionJob xml(@PathVariable Long id, @RequestParam("archivos") List<MultipartFile> archivos) {
        return service.importarXml(id, archivos);
    }

    @PostMapping("/metadata")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportacionJob metadata(@PathVariable Long id, @RequestParam("archivos") List<MultipartFile> archivos) {
        return service.importarMetadata(id, archivos);
    }

    @GetMapping
    public List<ImportacionJob> historial(@PathVariable Long id) { return service.historial(id); }

    @GetMapping("/{jobId}")
    public ImportacionJob estado(@PathVariable Long id, @PathVariable Long jobId) { return service.estado(id, jobId); }

    @GetMapping("/{jobId}/errores")
    public Page<ImportacionError> errores(@PathVariable Long id, @PathVariable Long jobId, Pageable p) {
        return service.bitacora(id, jobId, p);
    }
}
