package mx.culturafiscal.atipicos.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import mx.culturafiscal.atipicos.usuario.Usuario;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMs;
    private final long refreshMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessMs = accessMs;
        this.refreshMs = refreshMs;
    }

    public String generarAccessToken(Usuario u) {
        return construir(u, accessMs, "access");
    }

    public String generarRefreshToken(Usuario u) {
        return construir(u, refreshMs, "refresh");
    }

    public long getAccessMs() {
        return accessMs;
    }

    private String construir(Usuario u, long ms, String tipo) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("rol", u.getRol().name())
                .claim("typ", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + ms))
                .signWith(key)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean esAccess(Claims c) {
        return "access".equals(c.get("typ", String.class));
    }

    public boolean esRefresh(Claims c) {
        return "refresh".equals(c.get("typ", String.class));
    }
}
