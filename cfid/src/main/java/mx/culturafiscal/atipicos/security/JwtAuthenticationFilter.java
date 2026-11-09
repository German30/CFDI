package mx.culturafiscal.atipicos.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter{
    
    private final JwtService jwtService;
    private final UserDetailsService uds;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService uds) {
        this.jwtService = jwtService;
        this.uds = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException{
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if(header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            Claims claims = jwtService.validar(header.substring(7));
            if(jwtService.esAccess(claims) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails u = uds.loadUserByUsername(claims.getSubject());
                if(u.isEnabled()) {
                    var auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, res);
    }
}
