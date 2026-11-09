package mx.culturafiscal.atipicos.security;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service 
@RequiredArgsConstructor 
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository repo;

    @Override 
    public UserDetails loadUserByUsername(String email) {
        return repo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("usuario no encontrado"));
    }
}
