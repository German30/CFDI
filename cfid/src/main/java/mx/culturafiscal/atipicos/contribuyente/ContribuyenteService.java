package mx.culturafiscal.atipicos.contribuyente;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteDtos.ContribuyenteRequest;
import mx.culturafiscal.atipicos.datos.BorradoDatos;
import mx.culturafiscal.atipicos.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class ContribuyenteService {
    private final ContribuyenteRepository repo;
    private final BorradoDatos borrado;

    /** Devuelve el contribuyente solo si pertenece al usuario autenticado; de lo contrario, 404. */
    public Contribuyente propio(Long id) {
        return repo.findByIdAndUsuarioId(id, SecurityUtils.usuarioActual().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Contribuyente no encontrado"));
    }

    public List<Contribuyente> listar() {
        return repo.findByUsuarioIdOrderByRazonSocial(SecurityUtils.usuarioActual().getId());
    }

    public Contribuyente crear(ContribuyenteRequest r) {
        return crearPara(SecurityUtils.usuarioActual().getId(), r);
    }

    public Contribuyente crearPara(Long usuarioId, ContribuyenteRequest r) {
        String rfc = r.rfc().toUpperCase();
        if (repo.existsByUsuarioIdAndRfc(usuarioId, rfc)) throw new ReglaNegocioException("El RFC ya está registrado");
        Contribuyente c = Contribuyente.builder().usuarioId(usuarioId).rfc(rfc).build();
        aplicar(c, r);
        return repo.save(c);
    }

    public Contribuyente actualizar(Long id, ContribuyenteRequest r) {
        Contribuyente c = propio(id);
        if (!c.getRfc().equals(r.rfc().toUpperCase()))
            throw new ReglaNegocioException("El RFC no puede modificarse porque los CFDI se clasificaron con él");
        aplicar(c, r);
        return repo.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        Contribuyente c = propio(id);
        borrado.borrarDatos(c.getId());
        repo.delete(c);
    }

    private void aplicar(Contribuyente c, ContribuyenteRequest r) {
        c.setRazonSocial(r.razonSocial());
        c.setRegimenFiscal(r.regimenFiscal());
        c.setEjercicio(r.ejercicio());
        c.setActividadEconomica(r.actividadEconomica());
        c.setClavesCongruentes(r.clavesCongruentes());
        c.setModificadoEn(LocalDateTime.now());
    }
}
