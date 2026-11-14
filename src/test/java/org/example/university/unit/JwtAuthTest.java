package org.example.university.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.example.university.security.JwtAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthTest {

    private final JwtAuth jwtAuth = new JwtAuth("test-secret", 60_000);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void runFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", token));
        jwtAuth.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void validTokenAuthenticatesUserWithRole() throws Exception {
        String token = jwtAuth.createToken(new UsernamePasswordAuthenticationToken(
                "bob", null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

        runFilter(token);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("bob", auth.getName());
        assertEquals("ROLE_TEACHER", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void garbageTokenIsIgnored() throws Exception {
        runFilter("not.a.jwt");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenSignedWithOtherSecretIsIgnored() throws Exception {
        String foreign = new JwtAuth("other-secret", 60_000).createToken(new UsernamePasswordAuthenticationToken(
                "eve", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        runFilter(foreign);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
