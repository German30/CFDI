package mx.culturafiscal.atipicos.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.auth.AuthDtos.*;
import mx.culturafiscal.atipicos.security.SecurityUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) { return service.login(r); }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registro(@Valid @RequestBody RegistroRequest r) { return service.registrar(r); }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest r) { return service.refrescar(r); }

    @GetMapping("/me")
    public UsuarioDto me() { return UsuarioDto.de(SecurityUtils.usuarioActual()); }
}
