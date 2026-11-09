package mx.culturafiscal.atipicos.datos;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.Lista69bRepository;
import mx.culturafiscal.atipicos.security.SecurityUtils;

/** Eliminación total con doble confirmación: solicitar un token y confirmarlo con la frase exacta. */
@RestController
@RequestMapping("/api/datos")
@RequiredArgsConstructor
public class DatosController {
    public static final String FRASE = "ELIMINAR TODO";
    private static final long VIGENCIA_S = 300;

    private final BorradoDatos borrado;
    private final ContribuyenteRepository contribuyentes;
    private final Lista69bRepository lista69b;

    private record Solicitud(String token, Instant expira) { }
    private final Map<Long, Solicitud> solicitudes = new ConcurrentHashMap<>();
    private final SecureRandom azar = new SecureRandom();

    public record Confirmacion(String token, String frase) { }

    @PostMapping("/eliminacion")
    public Map<String, Object> solicitar() {
        byte[] b = new byte[24];
        azar.nextBytes(b);
        String t = HexFormat.of().formatHex(b);
        solicitudes.put(SecurityUtils.usuarioActual().getId(), new Solicitud(t, Instant.now().plusSeconds(VIGENCIA_S)));
        return Map.of("token", t, "vigenciaSegundos", VIGENCIA_S, "frase", FRASE);
    }

    @DeleteMapping("/eliminacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void confirmar(@RequestBody Confirmacion c) {
        Long u = SecurityUtils.usuarioActual().getId();
        Solicitud s = solicitudes.remove(u);
        if (s == null || s.expira().isBefore(Instant.now()) || !s.token().equals(c.token()) || !FRASE.equals(c.frase()))
            throw new ReglaNegocioException("Confirmación inválida o vencida; solicite una nueva");
        for (Contribuyente x : contribuyentes.findByUsuarioIdOrderByRazonSocial(u)) {
            borrado.borrarDatos(x.getId());
            contribuyentes.deleteById(x.getId());
        }
        lista69b.borrar(u);
    }
}
