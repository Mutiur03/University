package org.example.university.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.university.security.JwtAuth;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtAuth jwtAuth;

    public AuthController(AuthenticationManager authenticationManager, JwtAuth jwtAuth) {
        this.authenticationManager = authenticationManager;
        this.jwtAuth = jwtAuth;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
                "username", auth.getName(),
                "role", auth.getAuthorities().iterator().next().getAuthority());
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(jwtAuth.createToken(auth), jwtAuth.getExpirationMs())
                .toString());
        Map<String, String> result = new LinkedHashMap<>();
        result.put("username", username);
        result.put("role", auth.getAuthorities().iterator().next().getAuthority());
        return result;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    private static ResponseCookie buildCookie(String token, long maxAgeMs) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }
}
