package org.example.university.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ApiIntegrationTest {

    @Autowired WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    Filter springSecurityFilterChain;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    private Cookie login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    @Test
    void adminLoginSetsCookieAndMeReturnsRole() throws Exception {
        Cookie cookie = login("admin", "admin123");

        mvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void protectedEndpointWithoutLoginIs401() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesStudentWhoSeesOnlyOwnRecordAndCannotCreate() throws Exception {
        String email = "s-" + UUID.randomUUID() + "@x.com";
        Cookie admin = login("admin", "admin123");

        MvcResult created = mvc.perform(post("/api/students")
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String password = JsonPath.read(created.getResponse().getContentAsString(), "$.credentials.temporaryPassword");

        Cookie student = login(email, password);
        mvc.perform(get("/api/students").cookie(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(email));
        mvc.perform(post("/api/students")
                        .cookie(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\",\"lastName\":\"Y\",\"email\":\"z@x.com\"}"))
                .andExpect(status().isForbidden());
    }
}
