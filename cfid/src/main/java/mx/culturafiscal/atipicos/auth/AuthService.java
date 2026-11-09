package mx.culturafiscal.atipicos.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.auth.AuthDtos.*;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.security.JwtService;
import mx.culturafiscal.atipicos.usuario.Rol;
import mx.culturafiscal.atipicos.usuario.Usuario;
import mx.culturafiscal.atipicos.usuario.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authManager;
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public TokenResponse login(LoginRequest r) {
        String email = r.email().toLowerCase();
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, r.password()));
        return tokens(repo.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Credenciales inválidas")));
    }

    public TokenResponse registrar(RegistroRequest r) {
        String email = r.email().toLowerCase();
        if (repo.existsByEmail(email)) throw new ReglaNegocioException("El correo ya está registrado");
        return tokens(repo.save(Usuario.builder().nombre(r.nombre()).email(email)
                .password(encoder.encode(r.password())).rol(Rol.USUARIO).activo(true).build()));
    }

    public TokenResponse refrescar(RefreshRequest r) {
        try {
            Claims c = jwt.validar(r.refreshToken());
            if (!jwt.esRefresh(c)) throw new BadCredentialsException("Token inválido");
            Usuario u = repo.findByEmail(c.getSubject()).filter(Usuario::isEnabled)
                    .orElseThrow(() -> new BadCredentialsException("Token inválido"));
            return tokens(u);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Token inválido");
        }
    }

    private TokenResponse tokens(Usuario u) {
        return new TokenResponse(jwt.generarAccessToken(u), jwt.generarRefreshToken(u), jwt.getAccessMs(), UsuarioDto.de(u));
    }
}
