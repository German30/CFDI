package mx.culturafiscal.atipicos.exportacion;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;

@RestController
@RequestMapping("/api/contribuyentes/{id}/exportar")
@RequiredArgsConstructor
public class ExportacionController {
    private final ExportacionService service;
    private final ContribuyenteService contribuyentes;

    private ResponseEntity<byte[]> archivo(byte[] b, String nombre, MediaType t) {
        return ResponseEntity.ok().contentType(t)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString()).body(b);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        int e = ejercicio != null ? ejercicio : c.getEjercicio();
        return archivo(service.pdf(c, e), "Resumen_" + c.getRfc() + "_" + e + ".pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio) {
        Contribuyente c = contribuyentes.propio(id);
        int e = ejercicio != null ? ejercicio : c.getEjercicio();
        return archivo(service.excel(c, e), "Analisis_" + c.getRfc() + "_" + e + ".xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
