package mx.culturafiscal.atipicos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mx.culturafiscal.atipicos.usuario.Usuario;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record RegistroRequest(@NotBlank @Size(max = 150) String nombre, @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 72) String password) { }
    public record RefreshRequest(@NotBlank String refreshToken) { }
    public record TokenResponse(String accessToken, String refreshToken, long expiraEnMs, UsuarioDto usuario) { }
    public record UsuarioDto(Long id, String nombre, String email, String rol, boolean activo) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail(), u.getRol().name(), u.isActivo());
        }
    }
}
