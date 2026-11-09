package mx.culturafiscal.atipicos.malla;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.cfdi.Direccion;
import mx.culturafiscal.atipicos.cfdi.TipoComprobante;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteService;
import mx.culturafiscal.atipicos.malla.MallaDtos.*;

@RestController
@RequestMapping("/api/contribuyentes/{id}/malla")
@RequiredArgsConstructor
public class MallaController {
    private final MallaService service;
    private final ContribuyenteService contribuyentes;

    @GetMapping
    public Malla malla(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio,
            @RequestParam(required = false) TipoComprobante tipo, @RequestParam(required = false) Direccion direccion,
            @RequestParam(required = false) String estatus, @RequestParam(required = false) String contraparte,
            @RequestParam(required = false) String claveProdServ) {
        Contribuyente c = contribuyentes.propio(id);
        return service.construir(c, ejercicio != null ? ejercicio : c.getEjercicio(), new Filtros(tipo, direccion, estatus, contraparte, claveProdServ));
    }

    @GetMapping("/detalle")
    public DetallePagina detalle(@PathVariable Long id, @RequestParam(required = false) Integer ejercicio,
            @RequestParam(required = false) Integer mes, @RequestParam(required = false) TipoComprobante tipo,
            @RequestParam(required = false) Direccion direccion, @RequestParam(required = false) String estatus,
            @RequestParam(required = false) String contraparte, @RequestParam(required = false) String claveProdServ,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "100") int tamano) {
        Contribuyente c = contribuyentes.propio(id);
        return service.detalle(c, ejercicio != null ? ejercicio : c.getEjercicio(), new Filtros(tipo, direccion, estatus, contraparte, claveProdServ),
                mes, Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 1000));
    }
}
