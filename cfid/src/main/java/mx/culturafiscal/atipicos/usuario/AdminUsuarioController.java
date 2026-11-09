package mx.culturafiscal.atipicos.usuario;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.auth.AuthDtos.UsuarioDto;
import mx.culturafiscal.atipicos.common.exception.RecursoNoEncontradoException;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {
    private final UsuarioRepository repo;

    @GetMapping
    public List<UsuarioDto> listar() {
        return repo.findAll().stream().map(UsuarioDto::de).toList();
    }

    @PatchMapping("/{id}/activo")
    public UsuarioDto cambiarActivo(@PathVariable Long id, @RequestParam boolean activo) {
        Usuario u = repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        u.setActivo(activo);
        return UsuarioDto.de(repo.save(u));
    }
}
