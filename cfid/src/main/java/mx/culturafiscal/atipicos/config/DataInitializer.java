package mx.culturafiscal.atipicos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.culturafiscal.atipicos.usuario.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UsuarioRepository repo;

    private final PasswordEncoder encoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (!repo.existsByEmail(email)) {
            repo.save(Usuario.builder().nombre("Administrador").email(email)
                    .password(encoder.encode(password)).rol(Rol.ADMIN).activo(true).build());
            log.warn("Se creó el usuario administrador {}. Cambie la contraseña.", email);
        }
    }
}
