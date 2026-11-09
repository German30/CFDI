package mx.culturafiscal.atipicos.security;

import mx.culturafiscal.atipicos.usuario.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() { }

    public static Usuario usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof Usuario u)) throw new AccessDeniedException("No autenticado");
        return u;
    }
}
