package org.example.university.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.example.university.security.JwtAuth;
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
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        Map<String, String> response = new LinkedHashMap<>();
        response.put("username", username);
        response.put("role", auth.getAuthorities().iterator().next().getAuthority());
        response.put("accessToken", jwtAuth.createToken(auth));
        return response;
    }
}
