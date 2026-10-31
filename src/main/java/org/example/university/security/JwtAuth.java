package org.example.university.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuth extends OncePerRequestFilter {

    private final SecretKey key;
    private final long expirationMs;

    public JwtAuth(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.expirationMs = expirationMs;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String createToken(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        Date now = new Date();
        return Jwts.builder()
                .subject(authentication.getName())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            parse(header.substring(7)).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        user.username, null, List.of(new SimpleGrantedAuthority(user.role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<TokenUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);
            if (role == null) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new TokenUser(claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }

    private record TokenUser(String username, String role) {}
}
